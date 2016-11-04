package com.heliosapm.metrichub.speedment.tsdb.public_.tsd_tagpair.generated;

import com.heliosapm.metrichub.speedment.tsdb.public_.tsd_tagpair.TsdTagpair;
import com.speedment.runtime.config.identifier.TableIdentifier;
import com.speedment.runtime.core.manager.AbstractManager;
import com.speedment.runtime.field.Field;
import java.util.stream.Stream;
import javax.annotation.Generated;

/**
 * The generated base implementation for the manager of every {@link
 * com.heliosapm.metrichub.speedment.tsdb.public_.tsd_tagpair.TsdTagpair}
 * entity.
 * <p>
 * This file has been automatically generated by Speedment. Any changes made to
 * it will be overwritten.
 * 
 * @author Speedment
 */
@Generated("Speedment")
public abstract class GeneratedTsdTagpairManagerImpl extends AbstractManager<TsdTagpair> implements GeneratedTsdTagpairManager {
    
    private final TableIdentifier<TsdTagpair> tableIdentifier;
    
    protected GeneratedTsdTagpairManagerImpl() {
        this.tableIdentifier = TableIdentifier.of("tsdb", "public", "tsd_tagpair");
    }
    
    @Override
    public TableIdentifier<TsdTagpair> getTableIdentifier() {
        return tableIdentifier;
    }
    
    @Override
    public Stream<Field<TsdTagpair>> fields() {
        return Stream.of(
            TsdTagpair.XUID,
            TsdTagpair.TAGK,
            TsdTagpair.TAGV,
            TsdTagpair.NAME
        );
    }
    
    @Override
    public Stream<Field<TsdTagpair>> primaryKeyFields() {
        return Stream.of(
            TsdTagpair.XUID
        );
    }
}