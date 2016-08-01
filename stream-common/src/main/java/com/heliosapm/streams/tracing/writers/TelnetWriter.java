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
package com.heliosapm.streams.tracing.writers;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.heliosapm.streams.metrics.StreamedMetric;
import com.heliosapm.streams.metrics.StreamedMetricValue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * <p>Title: TelnetWriter</p>
 * <p>Description: </p> 
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.streams.tracing.writers.TelnetWriter</code></p>
 */

public class TelnetWriter extends NetWriter<NioSocketChannel> {
	
	/** The JVM's end of line separator */
	public static final String EOL = System.getProperty("line.separator");
	/** The UTF8 character set */
	public static final Charset UTF8 = Charset.forName("UTF8");
	/** A string encoder */
	public static final StringEncoder STR_ENCODER = new StringEncoder(UTF8);
	/** A streamed metric to string encoder */
	protected static final StreamedMetricEncoder METRIC_ENCODER = new StreamedMetricEncoder();
	/** The telnet channel initializer */
	protected static final ChannelInitializer<NioSocketChannel> CHANNEL_INIT = new ChannelInitializer<NioSocketChannel>() {
		@Override
		protected void initChannel(final NioSocketChannel ch) throws Exception {
			ch.pipeline().addLast("stringEncoder", STR_ENCODER);
			ch.pipeline().addLast("metricEncoder", METRIC_ENCODER);
		}
	};
	
	/**
	 * Creates a new TelnetWriter
	 */
	public TelnetWriter() {
		super(NioSocketChannel.class, false);
	}
	

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.streams.tracing.writers.NetWriter#getChannelInitializer()
	 */
	@Override
	protected ChannelInitializer<NioSocketChannel> getChannelInitializer() {
		return CHANNEL_INIT;
	}
	
	/**
	 * <p>Title: StreamedMetricEncoder</p>
	 * <p>Description: Encoder to encode streamed metrics into strings</p> 
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.streams.tracing.writers.TelnetWriter.StreamedMetricEncoder</code></p>
	 */
	@Sharable
	public static class StreamedMetricEncoder extends MessageToMessageEncoder<Object> {
		/** Instance logger */
		protected final Logger log = LogManager.getLogger(getClass());
		
		/**
		 * {@inheritDoc}
		 * @see io.netty.handler.codec.MessageToMessageEncoder#encode(io.netty.channel.ChannelHandlerContext, java.lang.Object, java.util.List)
		 */
		@Override
		protected void encode(final ChannelHandlerContext ctx, final Object msg, final List<Object> out) throws Exception {
			if(msg==null) return;
			if(msg instanceof ByteBuf) {
				final ByteBuf buff = (ByteBuf)msg;
				final StreamedMetric[] values = StreamedMetricValue.read(new ByteBufInputStream(buff));
				if(values.length > 0) {
					final StringBuilder b = new StringBuilder(values.length * 128);
					for(StreamedMetric sm: values) {
						b.append(sm.toOpenTSDBString()).append(EOL);
					}
					out.add(b);
				}
			} else if(msg instanceof StreamedMetric) {
				out.add(((StreamedMetric)msg).toOpenTSDBString());
			} else if(msg instanceof StreamedMetric[]) {
				final StreamedMetric[] values = (StreamedMetric[])msg;
				final StringBuilder b = new StringBuilder(values.length * 128);
				for(StreamedMetric sm: values) {
					b.append(sm.toOpenTSDBString()).append(EOL);
				}
				out.add(b);				
			}  else if(msg instanceof Collection) {
				final Collection<Object> objects = (Collection<Object>)msg;
				if(!objects.isEmpty()) {
					final StringBuilder b = new StringBuilder(objects.size() * 128);
					for(Object o: objects) {
						if(o != null && (o instanceof StreamedMetric)) {
							b.append(((StreamedMetric)o).toOpenTSDBString()).append(EOL);
						}
					}
					if(b.length()>0) {
						out.add(b);
					}
				}
			} else {
				log.warn("Unknown type submitted: [{}]", msg.getClass().getName());
			}
		}
	}

}
