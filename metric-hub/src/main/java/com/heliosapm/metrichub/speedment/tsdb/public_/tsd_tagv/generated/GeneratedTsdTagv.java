package com.heliosapm.metrichub.speedment.tsdb.public_.tsd_tagv.generated;

import com.heliosapm.metrichub.speedment.tsdb.public_.tsd_tagv.TsdTagv;
import com.speedment.runtime.config.identifier.ColumnIdentifier;
import com.speedment.runtime.config.identifier.TableIdentifier;
import com.speedment.runtime.core.util.OptionalUtil;
import com.speedment.runtime.field.ComparableField;
import com.speedment.runtime.field.IntField;
import com.speedment.runtime.field.StringField;
import com.speedment.runtime.typemapper.TypeMapper;
import java.sql.Timestamp;
import java.util.Optional;
import javax.annotation.Generated;

/**
 * The generated base for the {@link
 * com.heliosapm.metrichub.speedment.tsdb.public_.tsd_tagv.TsdTagv}-interface
 * representing entities of the {@code tsd_tagv}-table in the database.
 * <p>
 * This file has been automatically generated by Speedment. Any changes made to
 * it will be overwritten.
 * 
 * @author Speedment
 */
@Generated("Speedment")
public interface GeneratedTsdTagv {
    
    /**
     * This Field corresponds to the {@link TsdTagv} field that can be obtained
     * using the {@link TsdTagv#getXuid()} method.
     */
    final StringField<TsdTagv, String> XUID = StringField.create(
        Identifier.XUID,
        TsdTagv::getXuid,
        TsdTagv::setXuid,
        TypeMapper.identity(), 
        true
    );
    /**
     * This Field corresponds to the {@link TsdTagv} field that can be obtained
     * using the {@link TsdTagv#getVersion()} method.
     */
    final IntField<TsdTagv, Integer> VERSION = IntField.create(
        Identifier.VERSION,
        TsdTagv::getVersion,
        TsdTagv::setVersion,
        TypeMapper.primitive(), 
        false
    );
    /**
     * This Field corresponds to the {@link TsdTagv} field that can be obtained
     * using the {@link TsdTagv#getName()} method.
     */
    final StringField<TsdTagv, String> NAME = StringField.create(
        Identifier.NAME,
        TsdTagv::getName,
        TsdTagv::setName,
        TypeMapper.identity(), 
        true
    );
    /**
     * This Field corresponds to the {@link TsdTagv} field that can be obtained
     * using the {@link TsdTagv#getCreated()} method.
     */
    final ComparableField<TsdTagv, Timestamp, Timestamp> CREATED = ComparableField.create(
        Identifier.CREATED,
        TsdTagv::getCreated,
        TsdTagv::setCreated,
        TypeMapper.identity(), 
        false
    );
    /**
     * This Field corresponds to the {@link TsdTagv} field that can be obtained
     * using the {@link TsdTagv#getLastUpdate()} method.
     */
    final ComparableField<TsdTagv, Timestamp, Timestamp> LAST_UPDATE = ComparableField.create(
        Identifier.LAST_UPDATE,
        TsdTagv::getLastUpdate,
        TsdTagv::setLastUpdate,
        TypeMapper.identity(), 
        false
    );
    /**
     * This Field corresponds to the {@link TsdTagv} field that can be obtained
     * using the {@link TsdTagv#getDescription()} method.
     */
    final StringField<TsdTagv, String> DESCRIPTION = StringField.create(
        Identifier.DESCRIPTION,
        o -> OptionalUtil.unwrap(o.getDescription()),
        TsdTagv::setDescription,
        TypeMapper.identity(), 
        false
    );
    /**
     * This Field corresponds to the {@link TsdTagv} field that can be obtained
     * using the {@link TsdTagv#getDisplayName()} method.
     */
    final StringField<TsdTagv, String> DISPLAY_NAME = StringField.create(
        Identifier.DISPLAY_NAME,
        o -> OptionalUtil.unwrap(o.getDisplayName()),
        TsdTagv::setDisplayName,
        TypeMapper.identity(), 
        false
    );
    /**
     * This Field corresponds to the {@link TsdTagv} field that can be obtained
     * using the {@link TsdTagv#getNotes()} method.
     */
    final StringField<TsdTagv, String> NOTES = StringField.create(
        Identifier.NOTES,
        o -> OptionalUtil.unwrap(o.getNotes()),
        TsdTagv::setNotes,
        TypeMapper.identity(), 
        false
    );
    /**
     * This Field corresponds to the {@link TsdTagv} field that can be obtained
     * using the {@link TsdTagv#getCustom()} method.
     */
    final StringField<TsdTagv, String> CUSTOM = StringField.create(
        Identifier.CUSTOM,
        o -> OptionalUtil.unwrap(o.getCustom()),
        TsdTagv::setCustom,
        TypeMapper.identity(), 
        false
    );
    
    /**
     * Returns the xuid of this TsdTagv. The xuid field corresponds to the
     * database column tsdb.public.tsd_tagv.xuid.
     * 
     * @return the xuid of this TsdTagv
     */
    String getXuid();
    
    /**
     * Returns the version of this TsdTagv. The version field corresponds to the
     * database column tsdb.public.tsd_tagv.version.
     * 
     * @return the version of this TsdTagv
     */
    int getVersion();
    
    /**
     * Returns the name of this TsdTagv. The name field corresponds to the
     * database column tsdb.public.tsd_tagv.name.
     * 
     * @return the name of this TsdTagv
     */
    String getName();
    
    /**
     * Returns the created of this TsdTagv. The created field corresponds to the
     * database column tsdb.public.tsd_tagv.created.
     * 
     * @return the created of this TsdTagv
     */
    Timestamp getCreated();
    
    /**
     * Returns the lastUpdate of this TsdTagv. The lastUpdate field corresponds
     * to the database column tsdb.public.tsd_tagv.last_update.
     * 
     * @return the lastUpdate of this TsdTagv
     */
    Timestamp getLastUpdate();
    
    /**
     * Returns the description of this TsdTagv. The description field
     * corresponds to the database column tsdb.public.tsd_tagv.description.
     * 
     * @return the description of this TsdTagv
     */
    Optional<String> getDescription();
    
    /**
     * Returns the displayName of this TsdTagv. The displayName field
     * corresponds to the database column tsdb.public.tsd_tagv.display_name.
     * 
     * @return the displayName of this TsdTagv
     */
    Optional<String> getDisplayName();
    
    /**
     * Returns the notes of this TsdTagv. The notes field corresponds to the
     * database column tsdb.public.tsd_tagv.notes.
     * 
     * @return the notes of this TsdTagv
     */
    Optional<String> getNotes();
    
    /**
     * Returns the custom of this TsdTagv. The custom field corresponds to the
     * database column tsdb.public.tsd_tagv.custom.
     * 
     * @return the custom of this TsdTagv
     */
    Optional<String> getCustom();
    
    /**
     * Sets the xuid of this TsdTagv. The xuid field corresponds to the database
     * column tsdb.public.tsd_tagv.xuid.
     * 
     * @param xuid to set of this TsdTagv
     * @return     this TsdTagv instance
     */
    TsdTagv setXuid(String xuid);
    
    /**
     * Sets the version of this TsdTagv. The version field corresponds to the
     * database column tsdb.public.tsd_tagv.version.
     * 
     * @param version to set of this TsdTagv
     * @return        this TsdTagv instance
     */
    TsdTagv setVersion(int version);
    
    /**
     * Sets the name of this TsdTagv. The name field corresponds to the database
     * column tsdb.public.tsd_tagv.name.
     * 
     * @param name to set of this TsdTagv
     * @return     this TsdTagv instance
     */
    TsdTagv setName(String name);
    
    /**
     * Sets the created of this TsdTagv. The created field corresponds to the
     * database column tsdb.public.tsd_tagv.created.
     * 
     * @param created to set of this TsdTagv
     * @return        this TsdTagv instance
     */
    TsdTagv setCreated(Timestamp created);
    
    /**
     * Sets the lastUpdate of this TsdTagv. The lastUpdate field corresponds to
     * the database column tsdb.public.tsd_tagv.last_update.
     * 
     * @param lastUpdate to set of this TsdTagv
     * @return           this TsdTagv instance
     */
    TsdTagv setLastUpdate(Timestamp lastUpdate);
    
    /**
     * Sets the description of this TsdTagv. The description field corresponds
     * to the database column tsdb.public.tsd_tagv.description.
     * 
     * @param description to set of this TsdTagv
     * @return            this TsdTagv instance
     */
    TsdTagv setDescription(String description);
    
    /**
     * Sets the displayName of this TsdTagv. The displayName field corresponds
     * to the database column tsdb.public.tsd_tagv.display_name.
     * 
     * @param displayName to set of this TsdTagv
     * @return            this TsdTagv instance
     */
    TsdTagv setDisplayName(String displayName);
    
    /**
     * Sets the notes of this TsdTagv. The notes field corresponds to the
     * database column tsdb.public.tsd_tagv.notes.
     * 
     * @param notes to set of this TsdTagv
     * @return      this TsdTagv instance
     */
    TsdTagv setNotes(String notes);
    
    /**
     * Sets the custom of this TsdTagv. The custom field corresponds to the
     * database column tsdb.public.tsd_tagv.custom.
     * 
     * @param custom to set of this TsdTagv
     * @return       this TsdTagv instance
     */
    TsdTagv setCustom(String custom);
    
    enum Identifier implements ColumnIdentifier<TsdTagv> {
        
        XUID ("xuid"),
        VERSION ("version"),
        NAME ("name"),
        CREATED ("created"),
        LAST_UPDATE ("last_update"),
        DESCRIPTION ("description"),
        DISPLAY_NAME ("display_name"),
        NOTES ("notes"),
        CUSTOM ("custom");
        
        private final String columnName;
        private final TableIdentifier<TsdTagv> tableIdentifier;
        
        Identifier(String columnName) {
            this.columnName = columnName;
            this.tableIdentifier = TableIdentifier.of(getDbmsName(), getSchemaName(), getTableName());
        }
        
        @Override
        public String getDbmsName() {
            return "tsdb";
        }
        
        @Override
        public String getSchemaName() {
            return "public";
        }
        
        @Override
        public String getTableName() {
            return "tsd_tagv";
        }
        
        @Override
        public String getColumnName() {
            return this.columnName;
        }
        
        @Override
        public TableIdentifier<TsdTagv> asTableIdentifier() {
            return this.tableIdentifier;
        }
    }
}