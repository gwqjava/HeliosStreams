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
package com.heliosapm.streams.metrichub.results;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.pool.ChannelPool;

/**
 * <p>Title: RequestCompletion</p>
 * <p>Description: </p> 
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.streams.metrichub.results.RequestCompletion</code></p>
 */

public class RequestCompletion extends SimpleChannelInboundHandler<QueryResult> {
	/** Static class log */
	private static final Logger log = LogManager.getLogger(RequestCompletion.class);
	/** Completion latch */
	private final CountDownLatch latch;
	/** A possible throwable thrown in the pipeline */
	private volatile Throwable t = null;
	/** The completed results */
	private final List<QueryResult> results;
	/** The number of expected results */
	private int expected = 0;
	
	private final Set<Thread> waitingThreads = Collections.synchronizedSet(new HashSet<Thread>(1));
	
	/** The number of received results */
	private int received = 0;
	/** The pool to return a non-errored out channel to */
	private final ChannelPool pool;
	
	public RequestCompletion(final int queryCount, final ChannelPool pool) {
		expected = queryCount;
		latch = new CountDownLatch(queryCount);
		this.pool = pool;
		results = new ArrayList<QueryResult>(queryCount);
	}
	
	@Override
	protected void channelRead0(final ChannelHandlerContext ctx, final QueryResult msg) throws Exception {
		received++;
		latch.countDown();
		results.add(msg);
		if(received==expected) {
			pool.release(ctx.channel());			
		}
	}
	
	@Override
	public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
		t = cause;
		waitingThreads.parallelStream().forEach(t -> t.interrupt());
		LongStream.builder().add(latch.getCount()).build().parallel().forEach(l -> latch.countDown());
		final Channel channel = ctx.channel();
		channel.close();
	}
	
	public List<QueryResult> get() {
		if(t!=null) throw new RuntimeException("Query failure", t);
		try {
			waitingThreads.add(Thread.currentThread());
			if(latch.await(15, TimeUnit.SECONDS)) {
				waitingThreads.remove(Thread.currentThread());
				if(t!=null) throw new RuntimeException("Query failure", t);
				return results;
			} else {
				waitingThreads.remove(Thread.currentThread());
				if(t!=null) throw new RuntimeException("Query failure", t);
				throw new RuntimeException("Query timed out");
			}
		} catch (InterruptedException iex) {
			waitingThreads.remove(Thread.currentThread());
			if(t!=null) throw new RuntimeException("Query failure", t);
			throw new RuntimeException("Thread interrupted while waiting on Query result");
		} finally {
			waitingThreads.remove(Thread.currentThread());
		}
	}

}