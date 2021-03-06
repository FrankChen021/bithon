/*
 * This file is generated by jOOQ.
 */
package org.bithon.server.storage.jdbc.jooq.tables;


import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import org.bithon.server.storage.jdbc.jooq.DefaultSchema;
import org.bithon.server.storage.jdbc.jooq.Indexes;
import org.bithon.server.storage.jdbc.jooq.tables.records.BithonApplicationInstanceRecord;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row4;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;


/**
 * 应用
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class BithonApplicationInstance extends TableImpl<BithonApplicationInstanceRecord> {

    private static final long serialVersionUID = -1314576785;

    /**
     * The reference instance of <code>bithon_application_instance</code>
     */
    public static final BithonApplicationInstance BITHON_APPLICATION_INSTANCE = new BithonApplicationInstance();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<BithonApplicationInstanceRecord> getRecordType() {
        return BithonApplicationInstanceRecord.class;
    }

    /**
     * The column <code>bithon_application_instance.timestamp</code>. update time
     */
    public final TableField<BithonApplicationInstanceRecord, Timestamp> TIMESTAMP = createField(DSL.name("timestamp"), org.jooq.impl.SQLDataType.TIMESTAMP.nullable(false), this, "update time");

    /**
     * The column <code>bithon_application_instance.appName</code>.
     */
    public final TableField<BithonApplicationInstanceRecord, String> APPNAME = createField(DSL.name("appName"), org.jooq.impl.SQLDataType.VARCHAR(128).nullable(false), this, "");

    /**
     * The column <code>bithon_application_instance.appType</code>.
     */
    public final TableField<BithonApplicationInstanceRecord, String> APPTYPE = createField(DSL.name("appType"), org.jooq.impl.SQLDataType.VARCHAR(64).nullable(false), this, "");

    /**
     * The column <code>bithon_application_instance.instanceName</code>.
     */
    public final TableField<BithonApplicationInstanceRecord, String> INSTANCENAME = createField(DSL.name("instanceName"), org.jooq.impl.SQLDataType.VARCHAR(64).nullable(false), this, "");

    /**
     * Create a <code>bithon_application_instance</code> table reference
     */
    public BithonApplicationInstance() {
        this(DSL.name("bithon_application_instance"), null);
    }

    /**
     * Create an aliased <code>bithon_application_instance</code> table reference
     */
    public BithonApplicationInstance(String alias) {
        this(DSL.name(alias), BITHON_APPLICATION_INSTANCE);
    }

    /**
     * Create an aliased <code>bithon_application_instance</code> table reference
     */
    public BithonApplicationInstance(Name alias) {
        this(alias, BITHON_APPLICATION_INSTANCE);
    }

    private BithonApplicationInstance(Name alias, Table<BithonApplicationInstanceRecord> aliased) {
        this(alias, aliased, null);
    }

    private BithonApplicationInstance(Name alias, Table<BithonApplicationInstanceRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment("应用"));
    }

    public <O extends Record> BithonApplicationInstance(Table<O> child, ForeignKey<O, BithonApplicationInstanceRecord> key) {
        super(child, key, BITHON_APPLICATION_INSTANCE);
    }

    @Override
    public Schema getSchema() {
        return DefaultSchema.DEFAULT_SCHEMA;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.BITHON_APPLICATION_INSTANCE_IDX_APP_INSTANCE_NAME, Indexes.BITHON_APPLICATION_INSTANCE_IDX_APP_INSTANCE_TIMESTAMP);
    }

    @Override
    public BithonApplicationInstance as(String alias) {
        return new BithonApplicationInstance(DSL.name(alias), this);
    }

    @Override
    public BithonApplicationInstance as(Name alias) {
        return new BithonApplicationInstance(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public BithonApplicationInstance rename(String name) {
        return new BithonApplicationInstance(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public BithonApplicationInstance rename(Name name) {
        return new BithonApplicationInstance(name, null);
    }

    // -------------------------------------------------------------------------
    // Row4 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row4<Timestamp, String, String, String> fieldsRow() {
        return (Row4) super.fieldsRow();
    }
}
