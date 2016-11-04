package com.heliosapm.metrichub.speedment.tsdb.public_.tsd_tagk.generated;

import com.heliosapm.metrichub.speedment.tsdb.public_.tsd_tagk.TsdTagk;
import com.speedment.runtime.config.identifier.TableIdentifier;
import com.speedment.runtime.core.manager.AbstractManager;
import com.speedment.runtime.field.Field;
import java.util.stream.Stream;
import javax.annotation.Generated;

/**
 * The generated base implementation for the manager of every {@link
 * com.heliosapm.metrichub.speedment.tsdb.public_.tsd_tagk.TsdTagk} entity.
 * <p>
 * This file has been automatically generated by Speedment. Any changes made to
 * it will be overwritten.
 * 
 * @author Speedment
 */
@Generated("Speedment")
public abstract class GeneratedTsdTagkManagerImpl extends AbstractManager<TsdTagk> implements GeneratedTsdTagkManager {
    
    private final TableIdentifier<TsdTagk> tableIdentifier;
    
    protected GeneratedTsdTagkManagerImpl() {
        this.tableIdentifier = TableIdentifier.of("tsdb", "public", "tsd_tagk");
    }
    
    @Override
    public TableIdentifier<TsdTagk> getTableIdentifier() {
        return tableIdentifier;
    }
    
    @Override
    public Stream<Field<TsdTagk>> fields() {
        return Stream.of(
            TsdTagk.XUID,
            TsdTagk.VERSION,
            TsdTagk.NAME,
            TsdTagk.CREATED,
            TsdTagk.LAST_UPDATE,
            TsdTagk.DESCRIPTION,
            TsdTagk.DISPLAY_NAME,
            TsdTagk.NOTES,
            TsdTagk.CUSTOM
        );
    }
    
    @Override
    public Stream<Field<TsdTagk>> primaryKeyFields() {
        return Stream.of(
            TsdTagk.XUID
        );
    }
}