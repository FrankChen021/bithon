/*
 * This file is generated by jOOQ.
 */
package org.bithon.server.storage.jdbc.common.jooq;


import org.bithon.server.storage.jdbc.common.jooq.tables.BithonAlertChangeLog;
import org.bithon.server.storage.jdbc.common.jooq.tables.BithonAlertEvaluationLog;
import org.bithon.server.storage.jdbc.common.jooq.tables.BithonAlertObject;
import org.bithon.server.storage.jdbc.common.jooq.tables.BithonAlertRecord;
import org.bithon.server.storage.jdbc.common.jooq.tables.BithonApplicationInstance;
import org.bithon.server.storage.jdbc.common.jooq.tables.BithonEvent;
import org.bithon.server.storage.jdbc.common.jooq.tables.BithonMetaApplicationMetricMap;
import org.bithon.server.storage.jdbc.common.jooq.tables.BithonMetaSchema;
import org.bithon.server.storage.jdbc.common.jooq.tables.BithonTraceMapping;
import org.bithon.server.storage.jdbc.common.jooq.tables.BithonTraceSpan;
import org.bithon.server.storage.jdbc.common.jooq.tables.BithonTraceSpanSummary;
import org.bithon.server.storage.jdbc.common.jooq.tables.BithonTraceSpanTagIndex;
import org.bithon.server.storage.jdbc.common.jooq.tables.BithonWebDashboard;
import org.jooq.Index;
import org.jooq.OrderField;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;


/**
 * A class modelling indexes of tables in the default schema.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Indexes {

    // -------------------------------------------------------------------------
    // INDEX definitions
    // -------------------------------------------------------------------------

    public static final Index BITHON_ALERT_EVALUATION_LOG_BITHON_ALERT_EVALUATION_LOG_TIMESTAMP = Internal.createIndex(DSL.name("bithon_alert_evaluation_log_timestamp"), BithonAlertEvaluationLog.BITHON_ALERT_EVALUATION_LOG, new OrderField[] { BithonAlertEvaluationLog.BITHON_ALERT_EVALUATION_LOG.TIMESTAMP }, false);
    public static final Index BITHON_ALERT_EVALUATION_LOG_BITHON_ALERT_EVALUATION_LOG_TIMESTAMP_ID = Internal.createIndex(DSL.name("bithon_alert_evaluation_log_timestamp_id"), BithonAlertEvaluationLog.BITHON_ALERT_EVALUATION_LOG, new OrderField[] { BithonAlertEvaluationLog.BITHON_ALERT_EVALUATION_LOG.ALERT_ID }, false);
    public static final Index BITHON_ALERT_CHANGE_LOG_IDX_ALERT_CHANGE_LOG_ALERT_ID = Internal.createIndex(DSL.name("idx_alert_change_log_alert_id"), BithonAlertChangeLog.BITHON_ALERT_CHANGE_LOG, new OrderField[] { BithonAlertChangeLog.BITHON_ALERT_CHANGE_LOG.ALERT_ID }, false);
    public static final Index BITHON_ALERT_CHANGE_LOG_IDX_ALERT_CHANGE_LOG_CREATED_AT = Internal.createIndex(DSL.name("idx_alert_change_log_created_at"), BithonAlertChangeLog.BITHON_ALERT_CHANGE_LOG, new OrderField[] { BithonAlertChangeLog.BITHON_ALERT_CHANGE_LOG.CREATED_AT }, false);
    public static final Index BITHON_ALERT_OBJECT_IDX_ALERT_OBJECT_APP_NAME = Internal.createIndex(DSL.name("idx_alert_object_app_name"), BithonAlertObject.BITHON_ALERT_OBJECT, new OrderField[] { BithonAlertObject.BITHON_ALERT_OBJECT.APP_NAME }, false);
    public static final Index BITHON_ALERT_OBJECT_IDX_ALERT_OBJECT_UPDATED_AT = Internal.createIndex(DSL.name("idx_alert_object_updated_at"), BithonAlertObject.BITHON_ALERT_OBJECT, new OrderField[] { BithonAlertObject.BITHON_ALERT_OBJECT.UPDATED_AT }, false);
    public static final Index BITHON_APPLICATION_INSTANCE_IDX_APP_INSTANCE_TIMESTAMP = Internal.createIndex(DSL.name("idx_app_instance_timestamp"), BithonApplicationInstance.BITHON_APPLICATION_INSTANCE, new OrderField[] { BithonApplicationInstance.BITHON_APPLICATION_INSTANCE.TIMESTAMP }, false);
    public static final Index BITHON_ALERT_RECORD_IDX_BITHON_ALERT_RECORD_ALERT_ID = Internal.createIndex(DSL.name("idx_bithon_alert_record_alert_id"), BithonAlertRecord.BITHON_ALERT_RECORD, new OrderField[] { BithonAlertRecord.BITHON_ALERT_RECORD.ALERT_ID }, false);
    public static final Index BITHON_ALERT_RECORD_IDX_BITHON_ALERT_RECORD_CREATED_AT = Internal.createIndex(DSL.name("idx_bithon_alert_record_created_at"), BithonAlertRecord.BITHON_ALERT_RECORD, new OrderField[] { BithonAlertRecord.BITHON_ALERT_RECORD.CREATED_AT }, false);
    public static final Index BITHON_ALERT_RECORD_IDX_BITHON_ALERT_RECORD_ID = Internal.createIndex(DSL.name("idx_bithon_alert_record_id"), BithonAlertRecord.BITHON_ALERT_RECORD, new OrderField[] { BithonAlertRecord.BITHON_ALERT_RECORD.RECORD_ID }, false);
    public static final Index BITHON_EVENT_IDX_EVENT_1_TIMESTAMP = Internal.createIndex(DSL.name("idx_event_1_timestamp"), BithonEvent.BITHON_EVENT, new OrderField[] { BithonEvent.BITHON_EVENT.TIMESTAMP }, false);
    public static final Index BITHON_EVENT_IDX_EVENT_APPNAME = Internal.createIndex(DSL.name("idx_event_appName"), BithonEvent.BITHON_EVENT, new OrderField[] { BithonEvent.BITHON_EVENT.APPNAME }, false);
    public static final Index BITHON_EVENT_IDX_EVENT_INSTANCENAME = Internal.createIndex(DSL.name("idx_event_instanceName"), BithonEvent.BITHON_EVENT, new OrderField[] { BithonEvent.BITHON_EVENT.INSTANCENAME }, false);
    public static final Index BITHON_EVENT_IDX_EVENT_TYPE = Internal.createIndex(DSL.name("idx_event_type"), BithonEvent.BITHON_EVENT, new OrderField[] { BithonEvent.BITHON_EVENT.TYPE }, false);
    public static final Index BITHON_META_APPLICATION_METRIC_MAP_IDX_META_APPLICATION_METRIC_MAP = Internal.createIndex(DSL.name("idx_meta_application_metric_map"), BithonMetaApplicationMetricMap.BITHON_META_APPLICATION_METRIC_MAP, new OrderField[] { BithonMetaApplicationMetricMap.BITHON_META_APPLICATION_METRIC_MAP.APPLICATION, BithonMetaApplicationMetricMap.BITHON_META_APPLICATION_METRIC_MAP.SCHEMA }, false);
    public static final Index BITHON_META_APPLICATION_METRIC_MAP_IDX_META_APPLICATION_METRIC_MAP_TIMESTAMP = Internal.createIndex(DSL.name("idx_meta_application_metric_map_timestamp"), BithonMetaApplicationMetricMap.BITHON_META_APPLICATION_METRIC_MAP, new OrderField[] { BithonMetaApplicationMetricMap.BITHON_META_APPLICATION_METRIC_MAP.TIMESTAMP }, false);
    public static final Index BITHON_META_SCHEMA_IDX_META_SCHEMA_TIMESTAMP = Internal.createIndex(DSL.name("idx_meta_schema_timestamp"), BithonMetaSchema.BITHON_META_SCHEMA, new OrderField[] { BithonMetaSchema.BITHON_META_SCHEMA.TIMESTAMP }, false);
    public static final Index BITHON_TRACE_MAPPING_IDX_TRACE_MAPPING_USER_TX_ID = Internal.createIndex(DSL.name("idx_trace_mapping_user_tx_id"), BithonTraceMapping.BITHON_TRACE_MAPPING, new OrderField[] { BithonTraceMapping.BITHON_TRACE_MAPPING.USER_TX_ID }, false);
    public static final Index BITHON_TRACE_SPAN_IDX_TS_1_TRACEID = Internal.createIndex(DSL.name("idx_ts_1_traceId"), BithonTraceSpan.BITHON_TRACE_SPAN, new OrderField[] { BithonTraceSpan.BITHON_TRACE_SPAN.TRACEID }, false);
    public static final Index BITHON_TRACE_SPAN_IDX_TS_2_TIMESTAMP = Internal.createIndex(DSL.name("idx_ts_2_timestamp"), BithonTraceSpan.BITHON_TRACE_SPAN, new OrderField[] { BithonTraceSpan.BITHON_TRACE_SPAN.TIMESTAMP }, false);
    public static final Index BITHON_TRACE_SPAN_IDX_TS_3_APP_NAME = Internal.createIndex(DSL.name("idx_ts_3_app_name"), BithonTraceSpan.BITHON_TRACE_SPAN, new OrderField[] { BithonTraceSpan.BITHON_TRACE_SPAN.APPNAME }, false);
    public static final Index BITHON_TRACE_SPAN_IDX_TS_4_INSTANCENAME = Internal.createIndex(DSL.name("idx_ts_4_instanceName"), BithonTraceSpan.BITHON_TRACE_SPAN, new OrderField[] { BithonTraceSpan.BITHON_TRACE_SPAN.INSTANCENAME }, false);
    public static final Index BITHON_TRACE_SPAN_IDX_TS_5_NAME = Internal.createIndex(DSL.name("idx_ts_5_name"), BithonTraceSpan.BITHON_TRACE_SPAN, new OrderField[] { BithonTraceSpan.BITHON_TRACE_SPAN.NAME }, false);
    public static final Index BITHON_TRACE_SPAN_SUMMARY_IDX_TSS_1_TIMESTAMP = Internal.createIndex(DSL.name("idx_tss_1_timestamp"), BithonTraceSpanSummary.BITHON_TRACE_SPAN_SUMMARY, new OrderField[] { BithonTraceSpanSummary.BITHON_TRACE_SPAN_SUMMARY.TIMESTAMP }, false);
    public static final Index BITHON_TRACE_SPAN_SUMMARY_IDX_TSS_2_APP_NAME = Internal.createIndex(DSL.name("idx_tss_2_app_name"), BithonTraceSpanSummary.BITHON_TRACE_SPAN_SUMMARY, new OrderField[] { BithonTraceSpanSummary.BITHON_TRACE_SPAN_SUMMARY.APPNAME }, false);
    public static final Index BITHON_TRACE_SPAN_SUMMARY_IDX_TSS_3_INSTANCENAME = Internal.createIndex(DSL.name("idx_tss_3_instanceName"), BithonTraceSpanSummary.BITHON_TRACE_SPAN_SUMMARY, new OrderField[] { BithonTraceSpanSummary.BITHON_TRACE_SPAN_SUMMARY.INSTANCENAME }, false);
    public static final Index BITHON_TRACE_SPAN_SUMMARY_IDX_TSS_4_TRACEID = Internal.createIndex(DSL.name("idx_tss_4_traceId"), BithonTraceSpanSummary.BITHON_TRACE_SPAN_SUMMARY, new OrderField[] { BithonTraceSpanSummary.BITHON_TRACE_SPAN_SUMMARY.TRACEID }, false);
    public static final Index BITHON_TRACE_SPAN_TAG_INDEX_IDX_TSTI_TIMESTAMP = Internal.createIndex(DSL.name("idx_tsti_timestamp"), BithonTraceSpanTagIndex.BITHON_TRACE_SPAN_TAG_INDEX, new OrderField[] { BithonTraceSpanTagIndex.BITHON_TRACE_SPAN_TAG_INDEX.TIMESTAMP }, false);
    public static final Index BITHON_WEB_DASHBOARD_IDX_WEB_DASHBOARD_TIMESTAMP = Internal.createIndex(DSL.name("idx_web_dashboard_timestamp"), BithonWebDashboard.BITHON_WEB_DASHBOARD, new OrderField[] { BithonWebDashboard.BITHON_WEB_DASHBOARD.TIMESTAMP }, false);
}
