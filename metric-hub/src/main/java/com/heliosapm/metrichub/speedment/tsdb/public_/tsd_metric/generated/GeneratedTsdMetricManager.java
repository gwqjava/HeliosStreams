package com.heliosapm.metrichub.speedment.tsdb.public_.tsd_metric.generated;

import com.heliosapm.metrichub.speedment.tsdb.public_.tsd_metric.TsdMetric;
import com.speedment.runtime.core.manager.Manager;
import javax.annotation.Generated;

/**
 * The generated base interface for the manager of every {@link
 * com.heliosapm.metrichub.speedment.tsdb.public_.tsd_metric.TsdMetric} entity.
 * <p>
 * This file has been automatically generated by Speedment. Any changes made to
 * it will be overwritten.
 * 
 * @author Speedment
 */
@Generated("Speedment")
public interface GeneratedTsdMetricManager extends Manager<TsdMetric> {
    
    @Override
    default Class<TsdMetric> getEntityClass() {
        return TsdMetric.class;
    }
}