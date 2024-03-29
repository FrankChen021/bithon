/*
 * This file is generated by jOOQ.
 */
package org.bithon.server.storage.jdbc.common.jooq;


import org.bithon.server.storage.jdbc.common.jooq.tables.BithonAgentSetting;
import org.bithon.server.storage.jdbc.common.jooq.tables.BithonAlertChangeLog;
import org.bithon.server.storage.jdbc.common.jooq.tables.BithonAlertEvaluationLog;
import org.bithon.server.storage.jdbc.common.jooq.tables.BithonAlertNotificationChannel;
import org.bithon.server.storage.jdbc.common.jooq.tables.BithonAlertObject;
import org.bithon.server.storage.jdbc.common.jooq.tables.BithonAlertRecord;
import org.bithon.server.storage.jdbc.common.jooq.tables.BithonAlertState;
import org.bithon.server.storage.jdbc.common.jooq.tables.BithonApplicationInstance;
import org.bithon.server.storage.jdbc.common.jooq.tables.BithonEvent;
import org.bithon.server.storage.jdbc.common.jooq.tables.BithonMetaApplicationMetricMap;
import org.bithon.server.storage.jdbc.common.jooq.tables.BithonMetaSchema;
import org.bithon.server.storage.jdbc.common.jooq.tables.BithonMetricsBaseline;
import org.bithon.server.storage.jdbc.common.jooq.tables.BithonTraceMapping;
import org.bithon.server.storage.jdbc.common.jooq.tables.BithonTraceSpan;
import org.bithon.server.storage.jdbc.common.jooq.tables.BithonTraceSpanSummary;
import org.bithon.server.storage.jdbc.common.jooq.tables.BithonTraceSpanTagIndex;
import org.bithon.server.storage.jdbc.common.jooq.tables.BithonWebDashboard;
import org.jooq.Index;
import org.jooq.OrderField;
import org.jooq.impl.Internal;


/**
 * A class modelling indexes of tables of the <code></code> schema.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Indexes {

    // -------------------------------------------------------------------------
    // INDEX definitions
    // -------------------------------------------------------------------------

    public static final Index BITHON_AGENT_SETTING_KEY_APPNAME = Indexes0.BITHON_AGENT_SETTING_KEY_APPNAME;
    public static final Index BITHON_ALERT_CHANGE_LOG_IDX_ALERT_CHANGE_LOG_ALERT_ID = Indexes0.BITHON_ALERT_CHANGE_LOG_IDX_ALERT_CHANGE_LOG_ALERT_ID;
    public static final Index BITHON_ALERT_CHANGE_LOG_IDX_ALERT_CHANGE_LOG_CREATED_AT = Indexes0.BITHON_ALERT_CHANGE_LOG_IDX_ALERT_CHANGE_LOG_CREATED_AT;
    public static final Index BITHON_ALERT_EVALUATION_LOG_BITHON_ALERT_EVALUATION_LOG_TIMESTAMP = Indexes0.BITHON_ALERT_EVALUATION_LOG_BITHON_ALERT_EVALUATION_LOG_TIMESTAMP;
    public static final Index BITHON_ALERT_EVALUATION_LOG_BITHON_ALERT_EVALUATION_LOG_TIMESTAMP_ID = Indexes0.BITHON_ALERT_EVALUATION_LOG_BITHON_ALERT_EVALUATION_LOG_TIMESTAMP_ID;
    public static final Index BITHON_ALERT_NOTIFICATION_CHANNEL_ALERT_NOTIFICATION_CHANNEL_NAME = Indexes0.BITHON_ALERT_NOTIFICATION_CHANNEL_ALERT_NOTIFICATION_CHANNEL_NAME;
    public static final Index BITHON_ALERT_OBJECT_IDX_ALERT_OBJECT_APP_NAME = Indexes0.BITHON_ALERT_OBJECT_IDX_ALERT_OBJECT_APP_NAME;
    public static final Index BITHON_ALERT_OBJECT_IDX_ALERT_OBJECT_UPDATED_AT = Indexes0.BITHON_ALERT_OBJECT_IDX_ALERT_OBJECT_UPDATED_AT;
    public static final Index BITHON_ALERT_OBJECT_UQ_ALERT_OBJECT_ID = Indexes0.BITHON_ALERT_OBJECT_UQ_ALERT_OBJECT_ID;
    public static final Index BITHON_ALERT_RECORD_IDX_BITHON_ALERT_RECORD_ALERT_ID = Indexes0.BITHON_ALERT_RECORD_IDX_BITHON_ALERT_RECORD_ALERT_ID;
    public static final Index BITHON_ALERT_RECORD_IDX_BITHON_ALERT_RECORD_CREATED_AT = Indexes0.BITHON_ALERT_RECORD_IDX_BITHON_ALERT_RECORD_CREATED_AT;
    public static final Index BITHON_ALERT_RECORD_IDX_BITHON_ALERT_RECORD_ID = Indexes0.BITHON_ALERT_RECORD_IDX_BITHON_ALERT_RECORD_ID;
    public static final Index BITHON_ALERT_STATE_UQ_ALERT_ID = Indexes0.BITHON_ALERT_STATE_UQ_ALERT_ID;
    public static final Index BITHON_APPLICATION_INSTANCE_IDX_APP_INSTANCE_TIMESTAMP = Indexes0.BITHON_APPLICATION_INSTANCE_IDX_APP_INSTANCE_TIMESTAMP;
    public static final Index BITHON_APPLICATION_INSTANCE_UQ_NAME_TYPE_INSTANCE = Indexes0.BITHON_APPLICATION_INSTANCE_UQ_NAME_TYPE_INSTANCE;
    public static final Index BITHON_EVENT_IDX_EVENT_1_TIMESTAMP = Indexes0.BITHON_EVENT_IDX_EVENT_1_TIMESTAMP;
    public static final Index BITHON_EVENT_IDX_EVENT_APPNAME = Indexes0.BITHON_EVENT_IDX_EVENT_APPNAME;
    public static final Index BITHON_EVENT_IDX_EVENT_INSTANCENAME = Indexes0.BITHON_EVENT_IDX_EVENT_INSTANCENAME;
    public static final Index BITHON_EVENT_IDX_EVENT_TYPE = Indexes0.BITHON_EVENT_IDX_EVENT_TYPE;
    public static final Index BITHON_META_APPLICATION_METRIC_MAP_IDX_META_APPLICATION_METRIC_MAP = Indexes0.BITHON_META_APPLICATION_METRIC_MAP_IDX_META_APPLICATION_METRIC_MAP;
    public static final Index BITHON_META_APPLICATION_METRIC_MAP_IDX_META_APPLICATION_METRIC_MAP_TIMESTAMP = Indexes0.BITHON_META_APPLICATION_METRIC_MAP_IDX_META_APPLICATION_METRIC_MAP_TIMESTAMP;
    public static final Index BITHON_META_SCHEMA_IDX_META_SCHEMA_NAME = Indexes0.BITHON_META_SCHEMA_IDX_META_SCHEMA_NAME;
    public static final Index BITHON_META_SCHEMA_IDX_META_SCHEMA_TIMESTAMP = Indexes0.BITHON_META_SCHEMA_IDX_META_SCHEMA_TIMESTAMP;
    public static final Index BITHON_METRICS_BASELINE_BITHON_METRICS_BASELINE_DATE = Indexes0.BITHON_METRICS_BASELINE_BITHON_METRICS_BASELINE_DATE;
    public static final Index BITHON_TRACE_MAPPING_IDX_TRACE_MAPPING_USER_TX_ID = Indexes0.BITHON_TRACE_MAPPING_IDX_TRACE_MAPPING_USER_TX_ID;
    public static final Index BITHON_TRACE_SPAN_IDX_TS_1_TRACEID = Indexes0.BITHON_TRACE_SPAN_IDX_TS_1_TRACEID;
    public static final Index BITHON_TRACE_SPAN_IDX_TS_2_TIMESTAMP = Indexes0.BITHON_TRACE_SPAN_IDX_TS_2_TIMESTAMP;
    public static final Index BITHON_TRACE_SPAN_IDX_TS_3_APP_NAME = Indexes0.BITHON_TRACE_SPAN_IDX_TS_3_APP_NAME;
    public static final Index BITHON_TRACE_SPAN_IDX_TS_4_INSTANCENAME = Indexes0.BITHON_TRACE_SPAN_IDX_TS_4_INSTANCENAME;
    public static final Index BITHON_TRACE_SPAN_IDX_TS_5_NAME = Indexes0.BITHON_TRACE_SPAN_IDX_TS_5_NAME;
    public static final Index BITHON_TRACE_SPAN_SUMMARY_IDX_TSS_1_TIMESTAMP = Indexes0.BITHON_TRACE_SPAN_SUMMARY_IDX_TSS_1_TIMESTAMP;
    public static final Index BITHON_TRACE_SPAN_SUMMARY_IDX_TSS_2_APP_NAME = Indexes0.BITHON_TRACE_SPAN_SUMMARY_IDX_TSS_2_APP_NAME;
    public static final Index BITHON_TRACE_SPAN_SUMMARY_IDX_TSS_3_INSTANCENAME = Indexes0.BITHON_TRACE_SPAN_SUMMARY_IDX_TSS_3_INSTANCENAME;
    public static final Index BITHON_TRACE_SPAN_SUMMARY_IDX_TSS_4_TRACEID = Indexes0.BITHON_TRACE_SPAN_SUMMARY_IDX_TSS_4_TRACEID;
    public static final Index BITHON_TRACE_SPAN_TAG_INDEX_IDX_TSTI_TIMESTAMP = Indexes0.BITHON_TRACE_SPAN_TAG_INDEX_IDX_TSTI_TIMESTAMP;
    public static final Index BITHON_WEB_DASHBOARD_IDX_WEB_DASHBOARD_NAME = Indexes0.BITHON_WEB_DASHBOARD_IDX_WEB_DASHBOARD_NAME;
    public static final Index BITHON_WEB_DASHBOARD_IDX_WEB_DASHBOARD_TIMESTAMP = Indexes0.BITHON_WEB_DASHBOARD_IDX_WEB_DASHBOARD_TIMESTAMP;

    // -------------------------------------------------------------------------
    // [#1459] distribute members to avoid static initialisers > 64kb
    // -------------------------------------------------------------------------

    private static class Indexes0 {
        public static Index BITHON_AGENT_SETTING_KEY_APPNAME = Internal.createIndex("key_appName", BithonAgentSetting.BITHON_AGENT_SETTING, new OrderField[] { BithonAgentSetting.BITHON_AGENT_SETTING.APPNAME, BithonAgentSetting.BITHON_AGENT_SETTING.SETTINGNAME }, true);
        public static Index BITHON_ALERT_CHANGE_LOG_IDX_ALERT_CHANGE_LOG_ALERT_ID = Internal.createIndex("idx_alert_change_log_alert_id", BithonAlertChangeLog.BITHON_ALERT_CHANGE_LOG, new OrderField[] { BithonAlertChangeLog.BITHON_ALERT_CHANGE_LOG.ALERT_ID }, false);
        public static Index BITHON_ALERT_CHANGE_LOG_IDX_ALERT_CHANGE_LOG_CREATED_AT = Internal.createIndex("idx_alert_change_log_created_at", BithonAlertChangeLog.BITHON_ALERT_CHANGE_LOG, new OrderField[] { BithonAlertChangeLog.BITHON_ALERT_CHANGE_LOG.CREATED_AT }, false);
        public static Index BITHON_ALERT_EVALUATION_LOG_BITHON_ALERT_EVALUATION_LOG_TIMESTAMP = Internal.createIndex("bithon_alert_evaluation_log_timestamp", BithonAlertEvaluationLog.BITHON_ALERT_EVALUATION_LOG, new OrderField[] { BithonAlertEvaluationLog.BITHON_ALERT_EVALUATION_LOG.TIMESTAMP }, false);
        public static Index BITHON_ALERT_EVALUATION_LOG_BITHON_ALERT_EVALUATION_LOG_TIMESTAMP_ID = Internal.createIndex("bithon_alert_evaluation_log_timestamp_id", BithonAlertEvaluationLog.BITHON_ALERT_EVALUATION_LOG, new OrderField[] { BithonAlertEvaluationLog.BITHON_ALERT_EVALUATION_LOG.ALERT_ID }, false);
        public static Index BITHON_ALERT_NOTIFICATION_CHANNEL_ALERT_NOTIFICATION_CHANNEL_NAME = Internal.createIndex("alert_notification_channel_name", BithonAlertNotificationChannel.BITHON_ALERT_NOTIFICATION_CHANNEL, new OrderField[] { BithonAlertNotificationChannel.BITHON_ALERT_NOTIFICATION_CHANNEL.NAME }, true);
        public static Index BITHON_ALERT_OBJECT_IDX_ALERT_OBJECT_APP_NAME = Internal.createIndex("idx_alert_object_app_name", BithonAlertObject.BITHON_ALERT_OBJECT, new OrderField[] { BithonAlertObject.BITHON_ALERT_OBJECT.APP_NAME }, false);
        public static Index BITHON_ALERT_OBJECT_IDX_ALERT_OBJECT_UPDATED_AT = Internal.createIndex("idx_alert_object_updated_at", BithonAlertObject.BITHON_ALERT_OBJECT, new OrderField[] { BithonAlertObject.BITHON_ALERT_OBJECT.UPDATED_AT }, false);
        public static Index BITHON_ALERT_OBJECT_UQ_ALERT_OBJECT_ID = Internal.createIndex("uq_alert_object_id", BithonAlertObject.BITHON_ALERT_OBJECT, new OrderField[] { BithonAlertObject.BITHON_ALERT_OBJECT.ALERT_ID }, true);
        public static Index BITHON_ALERT_RECORD_IDX_BITHON_ALERT_RECORD_ALERT_ID = Internal.createIndex("idx_bithon_alert_record_alert_id", BithonAlertRecord.BITHON_ALERT_RECORD, new OrderField[] { BithonAlertRecord.BITHON_ALERT_RECORD.ALERT_ID }, false);
        public static Index BITHON_ALERT_RECORD_IDX_BITHON_ALERT_RECORD_CREATED_AT = Internal.createIndex("idx_bithon_alert_record_created_at", BithonAlertRecord.BITHON_ALERT_RECORD, new OrderField[] { BithonAlertRecord.BITHON_ALERT_RECORD.CREATED_AT }, false);
        public static Index BITHON_ALERT_RECORD_IDX_BITHON_ALERT_RECORD_ID = Internal.createIndex("idx_bithon_alert_record_id", BithonAlertRecord.BITHON_ALERT_RECORD, new OrderField[] { BithonAlertRecord.BITHON_ALERT_RECORD.RECORD_ID }, false);
        public static Index BITHON_ALERT_STATE_UQ_ALERT_ID = Internal.createIndex("uq_alert_id", BithonAlertState.BITHON_ALERT_STATE, new OrderField[] { BithonAlertState.BITHON_ALERT_STATE.ALERT_ID }, true);
        public static Index BITHON_APPLICATION_INSTANCE_IDX_APP_INSTANCE_TIMESTAMP = Internal.createIndex("idx_app_instance_timestamp", BithonApplicationInstance.BITHON_APPLICATION_INSTANCE, new OrderField[] { BithonApplicationInstance.BITHON_APPLICATION_INSTANCE.TIMESTAMP }, false);
        public static Index BITHON_APPLICATION_INSTANCE_UQ_NAME_TYPE_INSTANCE = Internal.createIndex("uq_name_type_instance", BithonApplicationInstance.BITHON_APPLICATION_INSTANCE, new OrderField[] { BithonApplicationInstance.BITHON_APPLICATION_INSTANCE.APPNAME, BithonApplicationInstance.BITHON_APPLICATION_INSTANCE.APPTYPE, BithonApplicationInstance.BITHON_APPLICATION_INSTANCE.INSTANCENAME }, true);
        public static Index BITHON_EVENT_IDX_EVENT_1_TIMESTAMP = Internal.createIndex("idx_event_1_timestamp", BithonEvent.BITHON_EVENT, new OrderField[] { BithonEvent.BITHON_EVENT.TIMESTAMP }, false);
        public static Index BITHON_EVENT_IDX_EVENT_APPNAME = Internal.createIndex("idx_event_appName", BithonEvent.BITHON_EVENT, new OrderField[] { BithonEvent.BITHON_EVENT.APPNAME }, false);
        public static Index BITHON_EVENT_IDX_EVENT_INSTANCENAME = Internal.createIndex("idx_event_instanceName", BithonEvent.BITHON_EVENT, new OrderField[] { BithonEvent.BITHON_EVENT.INSTANCENAME }, false);
        public static Index BITHON_EVENT_IDX_EVENT_TYPE = Internal.createIndex("idx_event_type", BithonEvent.BITHON_EVENT, new OrderField[] { BithonEvent.BITHON_EVENT.TYPE }, false);
        public static Index BITHON_META_APPLICATION_METRIC_MAP_IDX_META_APPLICATION_METRIC_MAP = Internal.createIndex("idx_meta_application_metric_map", BithonMetaApplicationMetricMap.BITHON_META_APPLICATION_METRIC_MAP, new OrderField[] { BithonMetaApplicationMetricMap.BITHON_META_APPLICATION_METRIC_MAP.APPLICATION, BithonMetaApplicationMetricMap.BITHON_META_APPLICATION_METRIC_MAP.SCHEMA }, false);
        public static Index BITHON_META_APPLICATION_METRIC_MAP_IDX_META_APPLICATION_METRIC_MAP_TIMESTAMP = Internal.createIndex("idx_meta_application_metric_map_timestamp", BithonMetaApplicationMetricMap.BITHON_META_APPLICATION_METRIC_MAP, new OrderField[] { BithonMetaApplicationMetricMap.BITHON_META_APPLICATION_METRIC_MAP.TIMESTAMP }, false);
        public static Index BITHON_META_SCHEMA_IDX_META_SCHEMA_NAME = Internal.createIndex("idx_meta_schema_name", BithonMetaSchema.BITHON_META_SCHEMA, new OrderField[] { BithonMetaSchema.BITHON_META_SCHEMA.NAME }, true);
        public static Index BITHON_META_SCHEMA_IDX_META_SCHEMA_TIMESTAMP = Internal.createIndex("idx_meta_schema_timestamp", BithonMetaSchema.BITHON_META_SCHEMA, new OrderField[] { BithonMetaSchema.BITHON_META_SCHEMA.TIMESTAMP }, false);
        public static Index BITHON_METRICS_BASELINE_BITHON_METRICS_BASELINE_DATE = Internal.createIndex("bithon_metrics_baseline_date", BithonMetricsBaseline.BITHON_METRICS_BASELINE, new OrderField[] { BithonMetricsBaseline.BITHON_METRICS_BASELINE.DATE }, true);
        public static Index BITHON_TRACE_MAPPING_IDX_TRACE_MAPPING_USER_TX_ID = Internal.createIndex("idx_trace_mapping_user_tx_id", BithonTraceMapping.BITHON_TRACE_MAPPING, new OrderField[] { BithonTraceMapping.BITHON_TRACE_MAPPING.USER_TX_ID }, false);
        public static Index BITHON_TRACE_SPAN_IDX_TS_1_TRACEID = Internal.createIndex("idx_ts_1_traceId", BithonTraceSpan.BITHON_TRACE_SPAN, new OrderField[] { BithonTraceSpan.BITHON_TRACE_SPAN.TRACEID }, false);
        public static Index BITHON_TRACE_SPAN_IDX_TS_2_TIMESTAMP = Internal.createIndex("idx_ts_2_timestamp", BithonTraceSpan.BITHON_TRACE_SPAN, new OrderField[] { BithonTraceSpan.BITHON_TRACE_SPAN.TIMESTAMP }, false);
        public static Index BITHON_TRACE_SPAN_IDX_TS_3_APP_NAME = Internal.createIndex("idx_ts_3_app_name", BithonTraceSpan.BITHON_TRACE_SPAN, new OrderField[] { BithonTraceSpan.BITHON_TRACE_SPAN.APPNAME }, false);
        public static Index BITHON_TRACE_SPAN_IDX_TS_4_INSTANCENAME = Internal.createIndex("idx_ts_4_instanceName", BithonTraceSpan.BITHON_TRACE_SPAN, new OrderField[] { BithonTraceSpan.BITHON_TRACE_SPAN.INSTANCENAME }, false);
        public static Index BITHON_TRACE_SPAN_IDX_TS_5_NAME = Internal.createIndex("idx_ts_5_name", BithonTraceSpan.BITHON_TRACE_SPAN, new OrderField[] { BithonTraceSpan.BITHON_TRACE_SPAN.NAME }, false);
        public static Index BITHON_TRACE_SPAN_SUMMARY_IDX_TSS_1_TIMESTAMP = Internal.createIndex("idx_tss_1_timestamp", BithonTraceSpanSummary.BITHON_TRACE_SPAN_SUMMARY, new OrderField[] { BithonTraceSpanSummary.BITHON_TRACE_SPAN_SUMMARY.TIMESTAMP }, false);
        public static Index BITHON_TRACE_SPAN_SUMMARY_IDX_TSS_2_APP_NAME = Internal.createIndex("idx_tss_2_app_name", BithonTraceSpanSummary.BITHON_TRACE_SPAN_SUMMARY, new OrderField[] { BithonTraceSpanSummary.BITHON_TRACE_SPAN_SUMMARY.APPNAME }, false);
        public static Index BITHON_TRACE_SPAN_SUMMARY_IDX_TSS_3_INSTANCENAME = Internal.createIndex("idx_tss_3_instanceName", BithonTraceSpanSummary.BITHON_TRACE_SPAN_SUMMARY, new OrderField[] { BithonTraceSpanSummary.BITHON_TRACE_SPAN_SUMMARY.INSTANCENAME }, false);
        public static Index BITHON_TRACE_SPAN_SUMMARY_IDX_TSS_4_TRACEID = Internal.createIndex("idx_tss_4_traceId", BithonTraceSpanSummary.BITHON_TRACE_SPAN_SUMMARY, new OrderField[] { BithonTraceSpanSummary.BITHON_TRACE_SPAN_SUMMARY.TRACEID }, false);
        public static Index BITHON_TRACE_SPAN_TAG_INDEX_IDX_TSTI_TIMESTAMP = Internal.createIndex("idx_tsti_timestamp", BithonTraceSpanTagIndex.BITHON_TRACE_SPAN_TAG_INDEX, new OrderField[] { BithonTraceSpanTagIndex.BITHON_TRACE_SPAN_TAG_INDEX.TIMESTAMP }, false);
        public static Index BITHON_WEB_DASHBOARD_IDX_WEB_DASHBOARD_NAME = Internal.createIndex("idx_web_dashboard_name", BithonWebDashboard.BITHON_WEB_DASHBOARD, new OrderField[] { BithonWebDashboard.BITHON_WEB_DASHBOARD.NAME }, true);
        public static Index BITHON_WEB_DASHBOARD_IDX_WEB_DASHBOARD_TIMESTAMP = Internal.createIndex("idx_web_dashboard_timestamp", BithonWebDashboard.BITHON_WEB_DASHBOARD, new OrderField[] { BithonWebDashboard.BITHON_WEB_DASHBOARD.TIMESTAMP }, false);
    }
}
