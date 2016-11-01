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

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.heliosapm.streams.json.JSONOps;
import com.heliosapm.streams.tracing.TagKeySorter.TagMap;
import com.heliosapm.utils.url.URLHelper;

/**
 * <p>Title: QueryResult</p>
 * <p>Description: Represents the query results for one distinct metric in an <b>/api/query</b> query to OpenTSDB</p> 
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.streams.metrichub.results.QueryResult</code></p>
 */
@JsonDeserialize(using=QueryResultDeserializer.class)

public class QueryResult {
	/** The metric name */
	protected final String metricName;
	/** The metric tags */
	protected final Map<String, String> tags;
	/** Aggregated tags */
	protected final String[] aggregatedTags;
	/** The data points */
	protected final TreeSet<long[]> dps = new TreeSet<long[]>(DPS_COMPARATOR);
	
	
	
	/** Type reference for a tag key sorted map */
	public static final TypeReference<TagMap> TREE_MAP_TYPE_REF = new TypeReference<TagMap>(){};
	/** Type reference for a QueryResult array */
	public static final TypeReference<QueryResult[]> QR_ARR_TYPE_REF = new TypeReference<QueryResult[]>(){};
	
	
	
	
	
	/** Empty string array const */
	public static final String[] EMPTY_STR_ARR = {};
	/** Empty dps set const */
	public static final SortedSet<long[]> EMPTY_DPS_SET = Collections.unmodifiableSortedSet(new TreeSet<long[]>());
	
	
	/**
	 * <p>Title: DPSComparator</p>
	 * <p>Description: DPS array comparator</p> 
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.streams.metrichub.results.QueryResult.DPSComparator</code></p>
	 */
	private static class DPSComparator implements Comparator<long[]> {
		@Override
		public int compare(long[] o1, long[] o2) {			
			return o1[0] < o2[0] ? -1 : o1[0] > o2[0] ? 1 : 0;  
		}
	}
	
	/** DPS comparator */
	public static final Comparator<long[]> DPS_COMPARATOR = new DPSComparator();
	
	
	
	
	public QueryResult(final String metricName, final Map<String, String> tags, final String[] aggregatedTags, final Set<long[]> dps) {		
		this.metricName = metricName;
		this.tags = tags;
		this.aggregatedTags = aggregatedTags;
		this.dps.addAll(dps);

	}
	
	protected LongStream dpsStream(final int index) {
		final LongStream.Builder builder = LongStream.builder();
		StreamSupport.stream(dps.spliterator(), false).forEach(a -> builder.add(a[index]));
		return builder.build();
	}

	public LongStream times() {
		return dpsStream(0);
	}
	
	public LongStream values() {
		return dpsStream(1);
	}
	
	
//	public String toString() {
//		final StringBuilder b = new StringBuilder("QResult [\n\tm:")
//			.append(metricName).append(tags)
//			.append("\n\tAggTags:").append(Arrays.toString(aggregatedTags))
//			.append("\n\tDPS:");
//		for(long[] dp: dps) {
//			b.append("\n\t\t").append(new Date(dp[0])).append(" : ").append(dp[1]);
//		}
//		b.append("\n]");
//		return b.toString();
//	}
	
	public String toString() {
		if(dps.isEmpty()) {
			return new StringBuilder(metricName).append(":").append(tags).append(", aggtags:").append(Arrays.toString(aggregatedTags)).append(", dps:").append(dps.size()).toString();
		} else {
			final long[] first = dps.first();
			final long[] last = dps.last();
			final LongStream v = values();
			return new StringBuilder(metricName).append(":").append(tags).append(", aggtags:").append(Arrays.toString(aggregatedTags)).append(", dps:").append(dps.size())
					.append(", first:").append(first[1]).append(", last:").append(last[1])
					.append(", min:").append(values().min().getAsLong()).append(", max:").append(values().max().getAsLong()).append(", avg:").append(values().average().getAsDouble())
					.append("\n\t").append(values().summaryStatistics())
					.toString();
		}
	}
	
	
	public static void main(String[] args) {
		try {
			JSONOps.registerDeserializer(QueryResult.class, new QueryResultDeserializer());
			JSONOps.registerDeserializer(QueryResult[].class, new QueryResultArrayDeserializer());
			
			final String jsonResponse = URLHelper.getTextFromURL("./src/test/resources/responses/response-multi.js");
			final QueryResult[] qrs = JSONOps.parseToObject(jsonResponse, QueryResult[].class);
			System.out.println("Results:" + qrs.length);
			for(QueryResult q: qrs) {
				//System.out.println(q);
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}

}