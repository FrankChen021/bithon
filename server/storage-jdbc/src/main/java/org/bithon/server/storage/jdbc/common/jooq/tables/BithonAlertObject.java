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
import org.bithon.server.storage.jdbc.common.jooq.tables.records.BithonAlertObjectRecord;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row10;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;


/**
 * Alert
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class BithonAlertObject extends TableImpl<BithonAlertObjectRecord> {

    private static final long serialVersionUID = -26445323;

    /**
     * The reference instance of <code>bithon_alert_object</code>
     */
    public static final BithonAlertObject BITHON_ALERT_OBJECT = new BithonAlertObject();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<BithonAlertObjectRecord> getRecordType() {
        return BithonAlertObjectRecord.class;
    }

    /**
     * The column <code>bithon_alert_object.alert_id</code>. UUID
     */
    public final TableField<BithonAlertObjectRecord, String> ALERT_ID = createField(DSL.name("alert_id"), org.jooq.impl.SQLDataType.VARCHAR(32).nullable(false), this, "UUID");

    /**
     * The column <code>bithon_alert_object.alert_name</code>.
     */
    public final TableField<BithonAlertObjectRecord, String> ALERT_NAME = createField(DSL.name("alert_name"), org.jooq.impl.SQLDataType.VARCHAR(128).nullable(false).defaultValue(org.jooq.impl.DSL.inline("", org.jooq.impl.SQLDataType.VARCHAR)), this, "");

    /**
     * The column <code>bithon_alert_object.app_name</code>.
     */
    public final TableField<BithonAlertObjectRecord, String> APP_NAME = createField(DSL.name("app_name"), org.jooq.impl.SQLDataType.VARCHAR(128).nullable(false).defaultValue(org.jooq.impl.DSL.inline("", org.jooq.impl.SQLDataType.VARCHAR)), this, "");

    /**
     * The column <code>bithon_alert_object.namespace</code>. namespace of application
     */
    public final TableField<BithonAlertObjectRecord, String> NAMESPACE = createField(DSL.name("namespace"), org.jooq.impl.SQLDataType.VARCHAR(64).nullable(false), this, "namespace of application");

    /**
     * The column <code>bithon_alert_object.disabled</code>.
     */
    public final TableField<BithonAlertObjectRecord, Boolean> DISABLED = createField(DSL.name("disabled"), org.jooq.impl.SQLDataType.BOOLEAN.nullable(false).defaultValue(org.jooq.impl.DSL.inline("0", org.jooq.impl.SQLDataType.BOOLEAN)), this, "");

    /**
     * The column <code>bithon_alert_object.deleted</code>.
     */
    public final TableField<BithonAlertObjectRecord, Boolean> DELETED = createField(DSL.name("deleted"), org.jooq.impl.SQLDataType.BOOLEAN.nullable(false).defaultValue(org.jooq.impl.DSL.inline("0", org.jooq.impl.SQLDataType.BOOLEAN)), this, "");

    /**
     * The column <code>bithon_alert_object.payload</code>. JSON formatted alert
     */
    public final TableField<BithonAlertObjectRecord, String> PAYLOAD = createField(DSL.name("payload"), org.jooq.impl.SQLDataType.CLOB, this, "JSON formatted alert");

    /**
     * The column <code>bithon_alert_object.created_at</code>.
     */
    public final TableField<BithonAlertObjectRecord, LocalDateTime> CREATED_AT = createField(DSL.name("created_at"), org.jooq.impl.SQLDataType.LOCALDATETIME.nullable(false), this, "");

    /**
     * The column <code>bithon_alert_object.updated_at</code>.
     */
    public final TableField<BithonAlertObjectRecord, LocalDateTime> UPDATED_AT = createField(DSL.name("updated_at"), org.jooq.impl.SQLDataType.LOCALDATETIME.nullable(false), this, "");

    /**
     * The column <code>bithon_alert_object.last_operator</code>.
     */
    public final TableField<BithonAlertObjectRecord, String> LAST_OPERATOR = createField(DSL.name("last_operator"), org.jooq.impl.SQLDataType.VARCHAR(64).nullable(false).defaultValue(org.jooq.impl.DSL.inline("", org.jooq.impl.SQLDataType.VARCHAR)), this, "");

    /**
     * Create a <code>bithon_alert_object</code> table reference
     */
    public BithonAlertObject() {
        this(DSL.name("bithon_alert_object"), null);
    }

    /**
     * Create an aliased <code>bithon_alert_object</code> table reference
     */
    public BithonAlertObject(String alias) {
        this(DSL.name(alias), BITHON_ALERT_OBJECT);
    }

    /**
     * Create an aliased <code>bithon_alert_object</code> table reference
     */
    public BithonAlertObject(Name alias) {
        this(alias, BITHON_ALERT_OBJECT);
    }

    private BithonAlertObject(Name alias, Table<BithonAlertObjectRecord> aliased) {
        this(alias, aliased, null);
    }

    private BithonAlertObject(Name alias, Table<BithonAlertObjectRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment("Alert"));
    }

    public <O extends Record> BithonAlertObject(Table<O> child, ForeignKey<O, BithonAlertObjectRecord> key) {
        super(child, key, BITHON_ALERT_OBJECT);
    }

    @Override
    public Schema getSchema() {
        return DefaultSchema.DEFAULT_SCHEMA;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.BITHON_ALERT_OBJECT_IDX_ALERT_OBJECT_APP_NAME, Indexes.BITHON_ALERT_OBJECT_IDX_ALERT_OBJECT_UPDATED_AT, Indexes.BITHON_ALERT_OBJECT_UQ_ALERT_OBJECT_ID);
    }

    @Override
    public List<UniqueKey<BithonAlertObjectRecord>> getKeys() {
        return Arrays.<UniqueKey<BithonAlertObjectRecord>>asList(Keys.KEY_BITHON_ALERT_OBJECT_UQ_ALERT_OBJECT_ID);
    }

    @Override
    public BithonAlertObject as(String alias) {
        return new BithonAlertObject(DSL.name(alias), this);
    }

    @Override
    public BithonAlertObject as(Name alias) {
        return new BithonAlertObject(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public BithonAlertObject rename(String name) {
        return new BithonAlertObject(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public BithonAlertObject rename(Name name) {
        return new BithonAlertObject(name, null);
    }

    // -------------------------------------------------------------------------
    // Row10 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row10<String, String, String, String, Boolean, Boolean, String, LocalDateTime, LocalDateTime, String> fieldsRow() {
        return (Row10) super.fieldsRow();
    }
}