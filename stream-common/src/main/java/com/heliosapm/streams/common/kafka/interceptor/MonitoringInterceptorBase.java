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
package com.heliosapm.streams.common.kafka.interceptor;

import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;

import javax.management.ObjectName;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.cliffc.high_scale_lib.NonBlockingHashSet;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.heliosapm.streams.common.metrics.SharedMetricsRegistry;
import com.heliosapm.streams.common.naming.AgentName;
import com.heliosapm.streams.tracing.TagKeySorter;


/**
 * <p>Title: InterceptorBase</p>
 * <p>Description: The base class for the monitor interceptors</p> 
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.streams.common.kafka.interceptor.InterceptorBase</code></p>
 * @param <K> The message key type
 * @param <V> The message value type
 */

public abstract class MonitoringInterceptorBase<K, V> {
	/** The app name we're monitoring in */
	protected final String appName;
	/** The host name the app is running on */
	protected final String hostName;
	/** The service tags for shared metrics */
	protected String serviceTag;
	/** The service tags for shared metrics for producers with no partition */
	protected String noPartitionServiceTag;
	
	/** The group id for the consumer */
	protected String groupId = "";
	/** The client id for the consumer */
	protected String clientId = "";
	
	protected String commitKey = null;
	
	protected String jmxDomain = "com.heliosapm.streams.";
	protected final Hashtable<String, String> jmxProperties = new Hashtable<String, String>();
	protected ObjectName totalObjectName;
	protected ObjectName totalHistObjectName;
	
	/** The metrics registry */
	protected final SharedMetricsRegistry mr = SharedMetricsRegistry.getInstance();
	
	/** A map of counters keyed by the topic name + partition id */
	protected final NonBlockingHashSet<ObjectName> objectNames = new NonBlockingHashSet<ObjectName>(); 
	
	/** The total meter */
	protected Meter totalMeter;
	/** The total histogram */
	protected Histogram totalHistogram;
	
	/** True if this is a producer interceptor, false if a consumer */
	protected final boolean producer;
	/** The verb for this interceptor */
	protected final String verb;
	/** The noun for this interceptor */
	protected final String noun;
	
	private static final Meter PLACEHOLDER_METER = new Meter();
	private static final Histogram PLACEHOLDER_HISTOGRAM = new Histogram(null);
	
	/** A cache of meters keyed by topic + partition */
	protected final NonBlockingHashMap<String, Meter> cachedMeters = new NonBlockingHashMap<String, Meter>(); 
	/** A cache of histograms keyed by topic + partition */
	protected final NonBlockingHashMap<String, Histogram> cachedHistograms = new NonBlockingHashMap<String, Histogram>(); 

	/**
	 * Creates a new MonitoringInterceptorBase
	 * @param producer true for a producer, false for a consumer
	 */
	protected MonitoringInterceptorBase(final boolean producer) {
		this.producer = producer;
		verb = producer ? "produced" : "consumed";
		noun = producer ? "Producer" : "Consumer";
		jmxDomain = jmxDomain.concat(producer ? "producer" : "consumer");
		appName = AgentName.getInstance().getAppName();
		hostName = AgentName.getInstance().getHostName();
		jmxProperties.put("app", appName);
		jmxProperties.put("host", hostName);
//		totalMeter = SharedMetricsRegistry.getInstance().meter("messages." + verb + ".service=Kafka" + noun + "" + ".host=" + hostName + ".app=" + appName);
	}

	/**
	 * Acquires a meter for the passed topic name and partition id
	 * @param topicName The topic name
	 * @param partitionId The partition id
	 * @return the assigned meter
	 */
	protected Meter meter(final String topicName, final Integer partitionId) {		
		final long pId = partitionId==null ? -1L : partitionId;
		final String key = topicName + pId + producer;
		Meter meter = cachedMeters.putIfAbsent(key, PLACEHOLDER_METER);
		if(meter==null || meter==PLACEHOLDER_METER) {
			final Hashtable<String, String> props = new Hashtable<String, String>(jmxProperties);
			props.put("topic", topicName);
			if(pId!=-1L) {
				props.put("partition", "" + partitionId);
			}
			final ObjectName objectName = objectName(jmxDomain, props);	
			objectNames.add(objectName);
			meter = SharedMetricsRegistry.getInstance().mxMeter(objectName, "Msgs", "Kafka Client " + noun + " Messaging Rate Metrics");
			cachedMeters.replace(key, meter);
		}
		return meter;
	}
	
	/**
	 * Acquires a histogram for the passed topic name and partition id
	 * @param topicName The topic name
	 * @param partitionId The partition id
	 * @return the assigned histogram
	 */
	protected Histogram histogram(final String topicName, final Integer partitionId) {
		final long pId = partitionId==null ? -1L : partitionId;
		final String key = topicName + pId + producer;
		Histogram histogram = cachedHistograms.putIfAbsent(key, PLACEHOLDER_HISTOGRAM);
		if(histogram==null || histogram==PLACEHOLDER_HISTOGRAM) {
			final Hashtable<String, String> props = new Hashtable<String, String>(jmxProperties);
			props.put("topic", topicName);
			if(pId!=-1L) {
				props.put("partition", "" + partitionId);
			}
			final ObjectName objectName = objectName(jmxDomain, props);	
			@SuppressWarnings("unused")
			final boolean exists = objectNames.add(objectName);
			histogram = SharedMetricsRegistry.getInstance().mxHistogram(objectName, "Bytes", "Kafka Client " + noun + " Byte Transfer Metrics");
			cachedHistograms.replace(key, histogram);
		}
		return histogram;
	}
	
	
	/**
	 * Closes this interceptor 
	 */
	public void close() {
		// TODO: close the metrics in objectNames and cached metrics
	}
	
	/**
	 * Configures this interceptor
	 * @param configs The config properties
	 */
	public void configure(final Map<String, ?> configs) {
		final Object g =  configs.get("group.id");
		final Object c =  configs.get("client.id");
		groupId = g==null ? null : g.toString().trim();
		clientId = g==null ? null : c.toString().trim();
		final StringBuilder serviceBuilder = new StringBuilder("messages." + verb + ".service=Kafka" + noun + "" + ".host=" + hostName + ".app=" + appName + ".topic=%s.partition=%s");
		final StringBuilder noPartServiceBuilder = new StringBuilder("messages." + verb + ".service=Kafka" + noun + "" + ".host=" + hostName + ".app=" + appName + ".topic=%s");
		final StringBuilder commitKeyBuilder = new StringBuilder();
		if(clientId!=null && !clientId.trim().isEmpty()) {
			serviceBuilder.append(".client=%s");
			noPartServiceBuilder.append(".client=%s");
			clientId = clientId.trim();
			commitKeyBuilder.append(clientId);
//			jmxProperties.put("client", clientId);
		} else {
			serviceBuilder.append("%s");
			noPartServiceBuilder.append("%s");
			clientId = "";
		}
		if(groupId!=null && !groupId.trim().isEmpty()) {
			serviceBuilder.append(".group=%s");
			noPartServiceBuilder.append(".group=%s");
			groupId = groupId.trim();
			if(commitKeyBuilder.length()>0) commitKeyBuilder.append("."); 
			commitKeyBuilder.append(groupId);
//			jmxProperties.put("group", groupId);
		} else {
			serviceBuilder.append("%s");
			noPartServiceBuilder.append("%s");
			groupId = "";
		}
		commitKey = commitKeyBuilder.append("@").append(appName).append(".").append(hostName).toString();
		serviceTag = serviceBuilder.toString();		
		noPartitionServiceTag = noPartServiceBuilder.toString();
		totalObjectName = objectName(jmxDomain, jmxProperties);
		objectNames.add(totalObjectName);
		totalMeter = SharedMetricsRegistry.getInstance().mxMeter(totalObjectName, "Msgs", "Kafka Client " + noun + " Total Messaging Rate Metrics");
		if(producer) totalHistogram = SharedMetricsRegistry.getInstance().mxHistogram(totalObjectName, "Bytes", "Kafka Client " + noun + " Byte Transfer Metrics");
	}

	
	protected static ObjectName objectName(final String domain, final Hashtable<String, String> props) {
		final Map<String, String> map = new TreeMap<String, String>(TagKeySorter.INSTANCE);
		map.putAll(props);
		try {
			final StringBuilder b = new StringBuilder(domain).append(":");
			for(Map.Entry<String, String> entry: map.entrySet()) {
				b.append(entry.getKey()).append("=").append(entry.getValue()).append(",");
			}
			return new ObjectName(b.deleteCharAt(b.length()-1).toString());
		} catch (Exception ex) {
			throw new RuntimeException("Failed to create ObjectName for [" + domain + ":" + props);
		}
		
	}
	
	
}
