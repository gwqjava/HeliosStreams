 /**
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 */
package com.heliosapm.webrpc.subpub;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cliffc.high_scale_lib.NonBlockingHashSet;
import org.hbase.async.jsr166e.LongAdder;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.Funnels;
import com.google.common.hash.PrimitiveSink;
import com.heliosapm.utils.jmx.JMXHelper;
import com.heliosapm.webrpc.SingletonEnvironment;
import com.heliosapm.webrpc.serialization.Datapoint;

import net.opentsdb.meta.TSMeta;
import net.opentsdb.uid.UniqueId;
import reactor.core.Reactor;
import reactor.core.composable.Deferred;
import reactor.core.composable.Promise;
import reactor.core.composable.Stream;
import reactor.core.composable.spec.Promises;
import reactor.core.composable.spec.Streams;
import reactor.event.Event;
import reactor.event.dispatch.Dispatcher;
import reactor.event.registry.Registration;
import reactor.event.selector.Selector;
import reactor.event.selector.Selectors;
import reactor.function.Consumer;
import reactor.function.Function;
import reactor.timer.TimeUtils;
import reactor.tuple.Tuple2;


/**
 * <p>Title: Subscription</p>
 * <p>Description: Represents a SubscriptionManager subscription</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.webrpc.subpub.Subscription</code></p>
 */
public class Subscription implements SubscriptionMBean, Consumer<Map<String,Datapoint>>, SubscriberEventListener {
	/** Static class logger */
	protected static final Logger log = LogManager.getLogger(Subscription.class);

	/** The filter to quickly determine if an incoming message matches this subscription */
	private BloomFilter<byte[]> filter; 
	/** The charset of the incoming messages */
	public static final Charset DEFAULT_CHARSET = Charset.defaultCharset();
	/** The default number of insertions */
	public static final int DEFAULT_INSERTIONS = 10000;
	/** The ingestion funnel for incoming qualified messages */
	public enum SubFunnel implements Funnel<byte[]> {
	     /** The singleton funnel */
	    INSTANCE;	     
	     private final Funnel<byte[]> myfunnel = Funnels.byteArrayFunnel();

		@Override
		public void funnel(byte[] from, PrimitiveSink into) {
			myfunnel.funnel(from, into);
		}
	   }	
	
	/** The subscribers receiving notifications from this subscription */
	private final NonBlockingHashSet<Subscriber> subscribers = new NonBlockingHashSet<Subscriber>(); 
	
	/** The event feeder stream */
	private final Stream<Datapoint> stream;
	
	/** The flush stream definition */
	final Deferred<Map<String, Datapoint>, Stream<Map<String, Datapoint>>> flushDef;
	
	/** A serial number sequence for Subscription instances */
	private static final AtomicLong serial = new AtomicLong();
	
	/** The reactor  */
	protected final Reactor reactor;
	
	/** The reactor async dispatcher */
	protected final Dispatcher dispatcher;
	
	/** Registration for TSDBEvent stream */
	protected final Registration<Consumer<Event<TSDBEvent>>>  registration;
	/** Registration for subscriber termination event */
	protected final Registration<Consumer<Event<Subscriber<?>>>>  subTermRegistration;
	/** Registration for scheduled flush event */
	protected final Registration<? extends Consumer<Long>>  flushRegistration;
	
	
	/** The subscription pattern */
	protected final String pattern;
	/** The event type filter bit mask */
	protected final int eventBitMask;
	/** The subscription pattern as an ObjectName */
	protected final ObjectName patternObjectName;
	/** The subscription id for this subscription */
	protected final long subscriptionId;
	/** The initial expected insertions for the bloom filter */
	protected final int expectedInsertions;
	/** The selector for this subscription */
	protected final Selector selector;
	/** The datapoint accumulation map */
	private final Map<String, Datapoint> accumulation;
	/** The total number of matched incoming messages */
	protected final LongAdder totalMatched = new LongAdder();
	/** The total number of bloom filter "might" failures */
	protected final LongAdder mightDropped = new LongAdder();
	
	/** The current number of retained (inserted) patterns */
	protected final AtomicInteger retained = new AtomicInteger();
	
	/** The default false positive probability */
	public static final double DEFAULT_PROB = 0.3d;
	
	/**
	 * Creates a new Subscription
	 * @param reactor The reactor for event listening and async dispatch
	 * @param metricsMeta The metrics meta access service
	 * @param pattern The subscription pattern
	 * @param expectedInsertions The number of expected insertions
	 * @param types The TSDBEvent types to subscribe to
	 */
	public Subscription(final Reactor reactor, final CharSequence pattern, final int expectedInsertions, final TSDBEventType...types) {
		filter = BloomFilter.create(SubFunnel.INSTANCE, expectedInsertions, DEFAULT_PROB);
		this.pattern = pattern.toString().trim();
		selector = new TSMetaPatternSelector(this.pattern.toString());
		eventBitMask = TSDBEventType.getMask(types);
		this.expectedInsertions = expectedInsertions;
		accumulation = new ConcurrentHashMap<String, Datapoint>(this.expectedInsertions);
		this.reactor = reactor;
		this.dispatcher = reactor.getDispatcher();
		
		this.patternObjectName = JMXHelper.objectName(pattern);
		subscriptionId = serial.incrementAndGet();				
		flushDef = Streams.defer(SingletonEnvironment.getInstance().getEnv());
		
		final Deferred<TSDBEvent, Stream<TSDBEvent>> def = Streams.<TSDBEvent>defer(SingletonEnvironment.getInstance().getEnv());
		final Subscription self = this;
		stream = def.compose()
		.map(new Function<TSDBEvent, Datapoint>() {
			@Override
			public Datapoint apply(final TSDBEvent t) {				
				return new Datapoint(t);
			}
		})
		.consume(new Consumer<Datapoint>() {
			@Override
			public void accept(final Datapoint d) {				
				ingest(d);
			}
		});
		flushRegistration = TimeUtils.getTimer().schedule(new Consumer<Long>(){
			@Override
			public void accept(Long t) {
				self.accept(accumulation);
			}
		}, 5, TimeUnit.SECONDS, 5000);
//		flushStream.window(5000).map(new Function<List<Map<String,Datapoint>>, Map<String,Datapoint>>() {
//			@Override
//			public Map<String, Datapoint> apply(List<Map<String, Datapoint>> t) {
//				return accumulation;
//			}			
//		}).consume(this);
		registration = this.reactor.on(selector, new Consumer<Event<TSDBEvent>>() {
			@Override
			public void accept(final Event<TSDBEvent> t) {
				final TSDBEvent te = t.getData();
				if(te.eventType.isEnabled(eventBitMask)) {
					isMemberOf(te.tsuidBytes, te.metric, te.tags)
					.consume(new Consumer<Boolean>() {
						@Override
						public void accept(Boolean t) {
							if(t) {
								def.accept(te);
							}
						}
					});
				}				
			}
		});
		subTermRegistration = reactor.on(Selectors.object("subscriber-terminated"), new Consumer<Event<Subscriber<?>>>() {
			@Override
			public void accept(Event<Subscriber<?>> t) {
				final Subscriber<?> subscriber = t.getData();
				log.info("Removing Subscriber:\n\tSubscriber [{}] \n\tSubscription [{}]", subscriber, this);
				synchronized(subscribers) {
					if(subscribers.remove(subscriber)) {
						if(subscribers.isEmpty()) {
							log.info("Subscription terminating: [{}]", this);
							terminate();
							reactor.notify("subscription-terminated", Event.wrap(self));
						}
					}
				}
			}
		});
		flushDef.accept(accumulation);
		log.info("Subscription Graph\nConsumer [{}]:\n[{}]\n", System.identityHashCode(this), stream.debug());
	}
	
	/**
	 * {@inheritDoc}
	 * @see reactor.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(final Map<String,Datapoint> accumulatedDatapoints) {
		if(accumulatedDatapoints.isEmpty()) return;
		final HashMap<String,Datapoint> dc;
		synchronized(accumulatedDatapoints) {
			dc = new HashMap<String,Datapoint>(accumulatedDatapoints);
			accumulatedDatapoints.clear();
		}
		flushDef.accept(accumulation);
		log.info("Accumulated Datapoints:  [{}]", dc.size());
		dispatcher.execute(new Runnable() {
			public void run() {
				for(Subscriber s: subscribers) {
					s.accept(dc.values());
				}				
			}
		});
	}
	
	
	/**
	 * Aggregates a new datapoint into the current period's aggregation mao
	 * @param datapoint The datapoint to aggregate
	 */
	protected void ingest(final Datapoint datapoint) {
		Datapoint d = accumulation.get(datapoint.getFqn());
		if(d==null) {
			synchronized(accumulation) {
				d = accumulation.get(datapoint.getFqn());
				if(d==null) {
					accumulation.put(datapoint.getFqn(), datapoint);
				} else {
					d.apply(datapoint);
				}
			}
		} else {
			d.apply(datapoint);
		}
	}
	
	private final Function<Tuple2<TSDBEvent,Map<String, Datapoint>>,Map<String, Datapoint>> preWindowAccumulator = new Function<Tuple2<TSDBEvent,Map<String, Datapoint>>,Map<String, Datapoint>>() {
		@Override
		public Map<String, Datapoint> apply(Tuple2<TSDBEvent, Map<String, Datapoint>> t) {
			final TSDBEvent te = t.getT1();
			int merges = 0;
			int inserts = 0;
//			log.info("T2 null: {}", t.getT2()==null);
			final Map<String, Datapoint> accumulator = t.getT2()==null ? 
					new ConcurrentHashMap<String, Datapoint>() : 
					t.getT2();
			Datapoint d = accumulator.get(te.tsuid);
			if(d==null) {
				synchronized(accumulator) {
					d = accumulator.get(te.tsuid);
					if(d==null) {
						d = new Datapoint(te);
						accumulator.put(te.tsuid, d);		
						inserts++;
					} else {
						d.apply(te);
						merges++;
					}
				}
			} else {
				d.apply(te);
				merges++;
			}
//			log.info("Reduce:Size: {}, Inserts: {}, Merges:{}", accumulator.size(), inserts, merges);
			return accumulator;
		}
	};
	
	
	/**
	 * Creates a new Subscription
	 * @param reactor The reactor for event listening and async dispatch
	 * @param pattern The subscription pattern
	 * @param expectedInsertions The number of expected insertions
	 * @param types The TSDBEvent types to subscribe to
	 */
	public Subscription(final Reactor reactor, final ObjectName pattern, final int expectedInsertions, final TSDBEventType...types) {
		this(reactor, pattern.toString(), expectedInsertions);
	}
	
	
	/**
	 * Terminates this subscription
	 */
	public void terminate() {
		flushRegistration.cancel();
		registration.cancel();
		subTermRegistration.cancel();
		subscribers.clear();
	}
	
	/**
	 * Adds a subscriber to this subscription
	 * @param sub The subscriber to add
	 * @return the new total number of subscribers
	 */
	public int addSubscriber(final Subscriber sub) {
		if(sub==null) throw new IllegalArgumentException("The passed subscriber was null");
		subscribers.add(sub);
		sub.registerListener(this);			
		return subscribers.size();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriberEventListener#onDisconnect(org.helios.tsdb.plugins.remoting.subpub.Subscriber)
	 */
	@Override
	public void onDisconnect(Subscriber subscriber) {
		// TODO Auto-generated method stub
		
	}
	
	
	/**
	 * Removes a subscriber
	 * @param sub The subscriber to remove
	 * @return the new total number of subscribers
	 */
	public int removeSubscriber(final Subscriber sub) {
		if(sub==null) throw new IllegalArgumentException("The passed subscriber was null");
		subscribers.remove(sub);
		return subscribers.size();
	}
	
	
	/**
	 * Creates a new Subscription with the default expected insertions
	 * @param reactor The reactor for event listening and async dispatch
	 * @param pattern The subscription pattern
	 * @param types The TSDBEvent types to subscribe to
	 */
	public Subscription(final Reactor reactor, ObjectName pattern, final TSDBEventType...types) {
		this(reactor, pattern, DEFAULT_INSERTIONS);
	}
	
	/**
	 * Creates a new Subscription with the default expected insertions
	 * @param reactor The reactor for event listening and async dispatch
	 * @param pattern The subscription pattern
	 * @param types The TSDBEvent types to subscribe to
	 */
	public Subscription(final Reactor reactor, CharSequence pattern, final TSDBEventType...types) {
		this(reactor, pattern, DEFAULT_INSERTIONS);
	}
	
	
		
	
	/**
	 * Indicates if the passed message is a member of this subscription
	 * @param bytes The message to determine the membership of
	 * @param metric The metric name
	 * @param tags The metric tags
	 * @return false if the message is definitely NOT a member of this subscription, true if it is a member.
	 */
	public Promise<Boolean> isMemberOf(final byte[] bytes, final String metric, final Map<String, String> tags) {
		if(bytes==null) return Promises.success(false).get();
		final Deferred<Boolean, Promise<Boolean>> def = Promises.<Boolean>defer().dispatcher(this.dispatcher).get();
		final Promise<Boolean> promise = def.compose();
		dispatcher.execute(new Runnable() {
			public void run() {
				final long start = System.nanoTime();
				if(filter.mightContain(bytes)) {
					try {
						if(metric!=null && tags!=null) {
//							final String fqn = SubscriptionManager.buildObjectName(metric, tags).toString();							
//							metricsMeta.match(pattern, bytes).consume(new Consumer<Boolean>() {
//								@Override
//								public void accept(final Boolean t) {
//									final boolean b = t==null ? false : t;
//									def.accept(b);
//									if(b) {
//										index(bytes);
//										totalMatched.increment();
//									} else {
//										mightDropped.increment();
//									}
//									
//								}
//							}).when(Throwable.class, new Consumer<Throwable>() {
//								@Override
//								public void accept(final Throwable t) {
//									def.accept(t);
//									
//								}
//							});
						} else {
							def.accept(false);
						}						
					} catch (Exception ex) {
						def.accept(ex);
					}					
				} else {
					def.accept(false);
					
				}
			}
		});
		return promise;		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriptionMBean#test(java.lang.String)
	 */
	@Override
	public boolean test(String message) {
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean result = new AtomicBoolean(false);
		isMemberOf(message.getBytes(Charset.defaultCharset()), null, null).consume(new Consumer<Boolean>() {
			@Override
			public void accept(Boolean t) {
				result.set(t);
				latch.countDown();
			}
		}).when(Throwable.class, new Consumer<Throwable>() {
			@Override
			public void accept(Throwable t) {
				result.set(false);
				latch.countDown();				
			}
		});
		try {
			latch.await(3, TimeUnit.SECONDS);
		} catch (Exception ex) {
			/* No Op */
		}
		return result.get();
		
	}
	
	/**
	 * Indexes the passed message after it has been determined to be a member of this subscription 
	 * @param bytes the message to index
	 */
	public void index(final byte[] bytes) {
		if(bytes!=null) {
			if(filter.put(bytes)) {
				retained.incrementAndGet();
			}
		}
	}

	/**
	 * Indexes a time series id
	 * @param tsMeta the time series
	 */
	public void index(final TSMeta tsMeta) {		
		if(tsMeta!=null) {
			if(filter.put(UniqueId.stringToUid(tsMeta.getTSUID()))) {
				retained.incrementAndGet();
			}
		}		
	}
	

	/**
	 * Indexes a time series id. Internal version that does not trigger stats or pubs.
	 * @param tsuid the time series TSUID bytes
	 */
	void _internalIndex(final byte[] tsuid) {		
		if(tsuid!=null) {
			if(filter.put(tsuid)) {
				retained.incrementAndGet();
			}
		}		
	}

	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriptionMBean#getMatches()
	 */
	@Override
	public long getMatches() {
		return totalMatched.longValue();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriptionMBean#getDrops()
	 */
	@Override
	public long getDrops() {
		return mightDropped.longValue();
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriptionMBean#getSize()
	 */
	@Override
	public int getSize() {
		return retained.get();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriptionMBean#getErrorProbability()
	 */
	public double getErrorProbability() {
		return filter.expectedFpp();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriptionMBean#getRelativeProbability()
	 */
	@Override
	public double getRelativeProbability() {
		return DEFAULT_PROB - filter.expectedFpp();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriptionMBean#getSubscriberCount()
	 */
	@Override
	public int getSubscriberCount() {
		return subscribers.size();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriptionMBean#getSubscriptionId()
	 */
	@Override
	public long getSubscriptionId() {
		return subscriptionId;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriptionMBean#getDescription()
	 */
	@Override
	public String getDescription() {
		return toString();
	}
	
	

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Subscription [Id:");
		builder.append(subscriptionId);
		builder.append(", pattern:");
		builder.append(pattern);
		builder.append(", size:");
		builder.append(retained.get());
		builder.append(", subscribers:");
		builder.append(subscribers.size());
		builder.append("]");
		return builder.toString();
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (int) (subscriptionId ^ (subscriptionId >>> 32));
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Subscription other = (Subscription) obj;
		if (subscriptionId != other.subscriptionId)
			return false;
		return true;
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.subpub.SubscriptionMBean#getCapacity()
	 */
	@Override
	public int getCapacity() {
		return expectedInsertions;
	}






	
	
	
	
}
