/*
 * This file is generated by jOOQ.
 */
package org.bithon.server.storage.jdbc.common.jooq.tables;


import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.bithon.server.storage.jdbc.common.jooq.DefaultSchema;
import org.bithon.server.storage.jdbc.common.jooq.Indexes;
import org.bithon.server.storage.jdbc.common.jooq.Keys;
import org.bithon.server.storage.jdbc.common.jooq.tables.records.BithonAgentSettingRecord;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row7;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class BithonAgentSetting extends TableImpl<BithonAgentSettingRecord> {

    private static final long serialVersionUID = -1060097659;

    /**
     * The reference instance of <code>bithon_agent_setting</code>
     */
    public static final BithonAgentSetting BITHON_AGENT_SETTING = new BithonAgentSetting();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<BithonAgentSettingRecord> getRecordType() {
        return BithonAgentSettingRecord.class;
    }

    /**
     * The column <code>bithon_agent_setting.timestamp</code>. Created Timestamp
     */
    public final TableField<BithonAgentSettingRecord, LocalDateTime> TIMESTAMP = createField(DSL.name("timestamp"), org.jooq.impl.SQLDataType.LOCALDATETIME.nullable(false), this, "Created Timestamp");

    /**
     * The column <code>bithon_agent_setting.appName</code>.
     */
    public final TableField<BithonAgentSettingRecord, String> APPNAME = createField(DSL.name("appName"), org.jooq.impl.SQLDataType.VARCHAR(128).nullable(false), this, "");

    /**
     * The column <code>bithon_agent_setting.environment</code>.
     */
    public final TableField<BithonAgentSettingRecord, String> ENVIRONMENT = createField(DSL.name("environment"), org.jooq.impl.SQLDataType.VARCHAR(128).nullable(false), this, "");

    /**
     * The column <code>bithon_agent_setting.settingName</code>.
     */
    public final TableField<BithonAgentSettingRecord, String> SETTINGNAME = createField(DSL.name("settingName"), org.jooq.impl.SQLDataType.VARCHAR(64).nullable(false), this, "");

    /**
     * The column <code>bithon_agent_setting.setting</code>. Setting text
     */
    public final TableField<BithonAgentSettingRecord, String> SETTING = createField(DSL.name("setting"), org.jooq.impl.SQLDataType.CLOB, this, "Setting text");

    /**
     * The column <code>bithon_agent_setting.format</code>. Format of the Setting, can be either "json" or "yaml"
     */
    public final TableField<BithonAgentSettingRecord, String> FORMAT = createField(DSL.name("format"), org.jooq.impl.SQLDataType.VARCHAR(16).nullable(false), this, "Format of the Setting, can be either \"json\" or \"yaml\"");

    /**
     * The column <code>bithon_agent_setting.updatedAt</code>.
     */
    public final TableField<BithonAgentSettingRecord, LocalDateTime> UPDATEDAT = createField(DSL.name("updatedAt"), org.jooq.impl.SQLDataType.LOCALDATETIME.nullable(false), this, "");

    /**
     * Create a <code>bithon_agent_setting</code> table reference
     */
    public BithonAgentSetting() {
        this(DSL.name("bithon_agent_setting"), null);
    }

    /**
     * Create an aliased <code>bithon_agent_setting</code> table reference
     */
    public BithonAgentSetting(String alias) {
        this(DSL.name(alias), BITHON_AGENT_SETTING);
    }

    /**
     * Create an aliased <code>bithon_agent_setting</code> table reference
     */
    public BithonAgentSetting(Name alias) {
        this(alias, BITHON_AGENT_SETTING);
    }

    private BithonAgentSetting(Name alias, Table<BithonAgentSettingRecord> aliased) {
        this(alias, aliased, null);
    }

    private BithonAgentSetting(Name alias, Table<BithonAgentSettingRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""));
    }

    public <O extends Record> BithonAgentSetting(Table<O> child, ForeignKey<O, BithonAgentSettingRecord> key) {
        super(child, key, BITHON_AGENT_SETTING);
    }

    @Override
    public Schema getSchema() {
        return DefaultSchema.DEFAULT_SCHEMA;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.BITHON_AGENT_SETTING_KEY_APPNAME);
    }

    @Override
    public List<UniqueKey<BithonAgentSettingRecord>> getKeys() {
        return Arrays.<UniqueKey<BithonAgentSettingRecord>>asList(Keys.KEY_BITHON_AGENT_SETTING_KEY_APPNAME);
    }

    @Override
    public BithonAgentSetting as(String alias) {
        return new BithonAgentSetting(DSL.name(alias), this);
    }

    @Override
    public BithonAgentSetting as(Name alias) {
        return new BithonAgentSetting(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public BithonAgentSetting rename(String name) {
        return new BithonAgentSetting(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public BithonAgentSetting rename(Name name) {
        return new BithonAgentSetting(name, null);
    }

    // -------------------------------------------------------------------------
    // Row7 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row7<LocalDateTime, String, String, String, String, String, LocalDateTime> fieldsRow() {
        return (Row7) super.fieldsRow();
    }
}
