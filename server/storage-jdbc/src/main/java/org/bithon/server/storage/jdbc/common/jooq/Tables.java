/*
 * This file is generated by jOOQ.
 */
package org.bithon.server.storage.jdbc.common.jooq;


import org.bithon.server.storage.jdbc.common.jooq.tables.BithonAgentSetting;
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


/**
 * Convenience access to all tables in 
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Tables {

    /**
     * The table <code>bithon_agent_setting</code>.
     */
    public static final BithonAgentSetting BITHON_AGENT_SETTING = BithonAgentSetting.BITHON_AGENT_SETTING;

    /**
     * The table <code>bithon_application_instance</code>.
     */
    public static final BithonApplicationInstance BITHON_APPLICATION_INSTANCE = BithonApplicationInstance.BITHON_APPLICATION_INSTANCE;

    /**
     * The table <code>bithon_event</code>.
     */
    public static final BithonEvent BITHON_EVENT = BithonEvent.BITHON_EVENT;

    /**
     * The table <code>bithon_meta_application_metric_map</code>.
     */
    public static final BithonMetaApplicationMetricMap BITHON_META_APPLICATION_METRIC_MAP = BithonMetaApplicationMetricMap.BITHON_META_APPLICATION_METRIC_MAP;

    /**
     * The table <code>bithon_meta_schema</code>.
     */
    public static final BithonMetaSchema BITHON_META_SCHEMA = BithonMetaSchema.BITHON_META_SCHEMA;

    /**
     * This table keeps the date when the metrics will be kept for ever
     */
    public static final BithonMetricsBaseline BITHON_METRICS_BASELINE = BithonMetricsBaseline.BITHON_METRICS_BASELINE;

    /**
     * The table <code>bithon_trace_mapping</code>.
     */
    public static final BithonTraceMapping BITHON_TRACE_MAPPING = BithonTraceMapping.BITHON_TRACE_MAPPING;

    /**
     * The table <code>bithon_trace_span</code>.
     */
    public static final BithonTraceSpan BITHON_TRACE_SPAN = BithonTraceSpan.BITHON_TRACE_SPAN;

    /**
     * The table <code>bithon_trace_span_summary</code>.
     */
    public static final BithonTraceSpanSummary BITHON_TRACE_SPAN_SUMMARY = BithonTraceSpanSummary.BITHON_TRACE_SPAN_SUMMARY;

    /**
     * The table <code>bithon_trace_span_tag_index</code>.
     */
    public static final BithonTraceSpanTagIndex BITHON_TRACE_SPAN_TAG_INDEX = BithonTraceSpanTagIndex.BITHON_TRACE_SPAN_TAG_INDEX;

    /**
     * The table <code>bithon_web_dashboard</code>.
     */
    public static final BithonWebDashboard BITHON_WEB_DASHBOARD = BithonWebDashboard.BITHON_WEB_DASHBOARD;
}