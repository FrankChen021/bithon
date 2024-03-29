/*
 * This file is generated by jOOQ.
 */
package org.bithon.server.storage.jdbc.common.jooq.tables.records;


import java.time.LocalDateTime;

import org.bithon.server.storage.jdbc.common.jooq.tables.BithonAlertState;
import org.jooq.Field;
import org.jooq.Record3;
import org.jooq.Row3;
import org.jooq.impl.TableRecordImpl;


/**
 * Alerting State
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class BithonAlertStateRecord extends TableRecordImpl<BithonAlertStateRecord> implements Record3<String, LocalDateTime, String> {

    private static final long serialVersionUID = 721367876;

    /**
     * Setter for <code>bithon_alert_state.alert_id</code>.
     */
    public void setAlertId(String value) {
        set(0, value);
    }

    /**
     * Getter for <code>bithon_alert_state.alert_id</code>.
     */
    public String getAlertId() {
        return (String) get(0);
    }

    /**
     * Setter for <code>bithon_alert_state.last_alert_at</code>.
     */
    public void setLastAlertAt(LocalDateTime value) {
        set(1, value);
    }

    /**
     * Getter for <code>bithon_alert_state.last_alert_at</code>.
     */
    public LocalDateTime getLastAlertAt() {
        return (LocalDateTime) get(1);
    }

    /**
     * Setter for <code>bithon_alert_state.last_record_id</code>. The PK ID in bithon_alert_record table
     */
    public void setLastRecordId(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>bithon_alert_state.last_record_id</code>. The PK ID in bithon_alert_record table
     */
    public String getLastRecordId() {
        return (String) get(2);
    }

    // -------------------------------------------------------------------------
    // Record3 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row3<String, LocalDateTime, String> fieldsRow() {
        return (Row3) super.fieldsRow();
    }

    @Override
    public Row3<String, LocalDateTime, String> valuesRow() {
        return (Row3) super.valuesRow();
    }

    @Override
    public Field<String> field1() {
        return BithonAlertState.BITHON_ALERT_STATE.ALERT_ID;
    }

    @Override
    public Field<LocalDateTime> field2() {
        return BithonAlertState.BITHON_ALERT_STATE.LAST_ALERT_AT;
    }

    @Override
    public Field<String> field3() {
        return BithonAlertState.BITHON_ALERT_STATE.LAST_RECORD_ID;
    }

    @Override
    public String component1() {
        return getAlertId();
    }

    @Override
    public LocalDateTime component2() {
        return getLastAlertAt();
    }

    @Override
    public String component3() {
        return getLastRecordId();
    }

    @Override
    public String value1() {
        return getAlertId();
    }

    @Override
    public LocalDateTime value2() {
        return getLastAlertAt();
    }

    @Override
    public String value3() {
        return getLastRecordId();
    }

    @Override
    public BithonAlertStateRecord value1(String value) {
        setAlertId(value);
        return this;
    }

    @Override
    public BithonAlertStateRecord value2(LocalDateTime value) {
        setLastAlertAt(value);
        return this;
    }

    @Override
    public BithonAlertStateRecord value3(String value) {
        setLastRecordId(value);
        return this;
    }

    @Override
    public BithonAlertStateRecord values(String value1, LocalDateTime value2, String value3) {
        value1(value1);
        value2(value2);
        value3(value3);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached BithonAlertStateRecord
     */
    public BithonAlertStateRecord() {
        super(BithonAlertState.BITHON_ALERT_STATE);
    }

    /**
     * Create a detached, initialised BithonAlertStateRecord
     */
    public BithonAlertStateRecord(String alertId, LocalDateTime lastAlertAt, String lastRecordId) {
        super(BithonAlertState.BITHON_ALERT_STATE);

        set(0, alertId);
        set(1, lastAlertAt);
        set(2, lastRecordId);
    }
}
