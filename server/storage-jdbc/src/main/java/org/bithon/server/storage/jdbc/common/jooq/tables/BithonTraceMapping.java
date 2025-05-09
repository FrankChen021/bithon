/*
 * This file is generated by jOOQ.
 */
package org.bithon.server.storage.jdbc.common.jooq.tables;


import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.bithon.server.storage.jdbc.common.jooq.DefaultSchema;
import org.bithon.server.storage.jdbc.common.jooq.Indexes;
import org.bithon.server.storage.jdbc.common.jooq.tables.records.BithonTraceMappingRecord;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function3;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row3;
import org.jooq.Schema;
import org.jooq.SelectField;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class BithonTraceMapping extends TableImpl<BithonTraceMappingRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>bithon_trace_mapping</code>
     */
    public static final BithonTraceMapping BITHON_TRACE_MAPPING = new BithonTraceMapping();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<BithonTraceMappingRecord> getRecordType() {
        return BithonTraceMappingRecord.class;
    }

    /**
     * The column <code>bithon_trace_mapping.timestamp</code>. Milli Seconds
     */
    public final TableField<BithonTraceMappingRecord, LocalDateTime> TIMESTAMP = createField(DSL.name("timestamp"), SQLDataType.LOCALDATETIME(3).nullable(false).defaultValue(DSL.field("CURRENT_TIMESTAMP(3)", SQLDataType.LOCALDATETIME)), this, "Milli Seconds");

    /**
     * The column <code>bithon_trace_mapping.user_tx_id</code>. user side
     * transaction id
     */
    public final TableField<BithonTraceMappingRecord, String> USER_TX_ID = createField(DSL.name("user_tx_id"), SQLDataType.VARCHAR(64).nullable(false), this, "user side transaction id");

    /**
     * The column <code>bithon_trace_mapping.trace_id</code>. trace id in bithon
     */
    public final TableField<BithonTraceMappingRecord, String> TRACE_ID = createField(DSL.name("trace_id"), SQLDataType.VARCHAR(64).nullable(false), this, "trace id in bithon");

    private BithonTraceMapping(Name alias, Table<BithonTraceMappingRecord> aliased) {
        this(alias, aliased, null);
    }

    private BithonTraceMapping(Name alias, Table<BithonTraceMappingRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>bithon_trace_mapping</code> table reference
     */
    public BithonTraceMapping(String alias) {
        this(DSL.name(alias), BITHON_TRACE_MAPPING);
    }

    /**
     * Create an aliased <code>bithon_trace_mapping</code> table reference
     */
    public BithonTraceMapping(Name alias) {
        this(alias, BITHON_TRACE_MAPPING);
    }

    /**
     * Create a <code>bithon_trace_mapping</code> table reference
     */
    public BithonTraceMapping() {
        this(DSL.name("bithon_trace_mapping"), null);
    }

    public <O extends Record> BithonTraceMapping(Table<O> child, ForeignKey<O, BithonTraceMappingRecord> key) {
        super(child, key, BITHON_TRACE_MAPPING);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : DefaultSchema.DEFAULT_SCHEMA;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.asList(Indexes.BITHON_TRACE_MAPPING_IDX_TRACE_MAPPING_USER_TX_ID);
    }

    @Override
    public BithonTraceMapping as(String alias) {
        return new BithonTraceMapping(DSL.name(alias), this);
    }

    @Override
    public BithonTraceMapping as(Name alias) {
        return new BithonTraceMapping(alias, this);
    }

    @Override
    public BithonTraceMapping as(Table<?> alias) {
        return new BithonTraceMapping(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public BithonTraceMapping rename(String name) {
        return new BithonTraceMapping(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public BithonTraceMapping rename(Name name) {
        return new BithonTraceMapping(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public BithonTraceMapping rename(Table<?> name) {
        return new BithonTraceMapping(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row3 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row3<LocalDateTime, String, String> fieldsRow() {
        return (Row3) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function3<? super LocalDateTime, ? super String, ? super String, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function3<? super LocalDateTime, ? super String, ? super String, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
