/*
 *    Copyright 2020 bithon.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.bithon.server.storage.jdbc.tracing.reader;

import org.bithon.component.commons.utils.SupplierUtils;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.jooq.Record;

import java.util.function.Supplier;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/5/7 16:41
 */
public interface TraceSpanRecordAccessor {

    String getAppName(Record record);

    String getInstanceName(Record record);

    String getTraceId(Record record);

    String getSpanId(Record record);

    String getParentSpanId(Record record);

    long getStartTime(Record record);

    long getCostTime(Record record);

    long getEndTime(Record record);

    String getName(Record record);

    String getKind(Record record);

    String getMethod(Record record);

    String getClazz(Record record);

    String getStatus(Record record);

    String getNormalizedUrl(Record record);

    String getTags(Record record);

    Object getAttributes(Record record);

    class SpanSummaryTableRecordAccessor implements TraceSpanRecordAccessor {
        private final Supplier<Integer> APP_NAME;
        private final Supplier<Integer> INSTANCE_NAME;
        private final Supplier<Integer> TRACE_ID;
        private final Supplier<Integer> SPAN_ID;
        private final Supplier<Integer> PARENT_SPAN_ID;
        private final Supplier<Integer> START_TIME;
        private final Supplier<Integer> COST_TIME;
        private final Supplier<Integer> END_TIME;
        private final Supplier<Integer> NAME;
        private final Supplier<Integer> KIND;
        private final Supplier<Integer> NORMALIZED_URL;
        private final Supplier<Integer> TAGS;
        private final Supplier<Integer> ATTRIBUTES;
        private final Supplier<Integer> CLAZZ;
        private final Supplier<Integer> METHOD;
        private final Supplier<Integer> STATUS;

        {
            APP_NAME = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN_SUMMARY.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN_SUMMARY.APPNAME));
            INSTANCE_NAME = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN_SUMMARY.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN_SUMMARY.INSTANCENAME));
            TRACE_ID = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN_SUMMARY.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN_SUMMARY.TRACEID));
            SPAN_ID = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN_SUMMARY.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN_SUMMARY.SPANID));
            PARENT_SPAN_ID = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN_SUMMARY.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN_SUMMARY.PARENTSPANID));
            START_TIME = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN_SUMMARY.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN_SUMMARY.STARTTIMEUS));
            COST_TIME = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN_SUMMARY.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN_SUMMARY.COSTTIMEUS));
            END_TIME = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN_SUMMARY.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN_SUMMARY.ENDTIMEUS));
            NAME = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN_SUMMARY.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN_SUMMARY.NAME));
            KIND = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN_SUMMARY.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN_SUMMARY.KIND));
            NORMALIZED_URL = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN_SUMMARY.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN_SUMMARY.NORMALIZEDURL));
            TAGS = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN_SUMMARY.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN_SUMMARY.TAGS));
            ATTRIBUTES = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN_SUMMARY.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN_SUMMARY.ATTRIBUTES));
            CLAZZ = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN_SUMMARY.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN_SUMMARY.CLAZZ));
            METHOD = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN_SUMMARY.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN_SUMMARY.METHOD));
            STATUS = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN_SUMMARY.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN_SUMMARY.STATUS));
        }

        public String getAppName(Record record) {
            return (String) record.get(APP_NAME.get());
        }

        public String getInstanceName(Record record) {
            return (String) record.get(INSTANCE_NAME.get());
        }

        public String getTraceId(Record record) {
            return (String) record.get(TRACE_ID.get());
        }

        public String getSpanId(Record record) {
            return (String) record.get(SPAN_ID.get());
        }

        public String getParentSpanId(Record record) {
            return (String) record.get(PARENT_SPAN_ID.get());
        }

        public long getStartTime(Record record) {
            return MicrosecondsUtils.toMicroseconds(record.get(START_TIME.get()));
        }

        public long getCostTime(Record record) {
            return (long) record.get(COST_TIME.get());
        }

        public long getEndTime(Record record) {
            return (long) record.get(END_TIME.get());
        }

        public String getName(Record record) {
            return (String) record.get(NAME.get());
        }

        public String getKind(Record record) {
            return (String) record.get(KIND.get());
        }

        public String getMethod(Record record) {
            return (String) record.get(METHOD.get());
        }

        public String getClazz(Record record) {
            return (String) record.get(CLAZZ.get());
        }

        public String getStatus(Record record) {
            return (String) record.get(STATUS.get());
        }

        public String getNormalizedUrl(Record record) {
            return (String) record.get(NORMALIZED_URL.get());
        }

        public String getTags(Record record) {
            return (String) record.get(TAGS.get());
        }

        public Object getAttributes(Record record) {
            return record.get(ATTRIBUTES.get());
        }
    }

    class SpanTableRecordAccessor implements TraceSpanRecordAccessor {
        private final Supplier<Integer> APP_NAME;
        private final Supplier<Integer> INSTANCE_NAME;
        private final Supplier<Integer> TRACE_ID;
        private final Supplier<Integer> SPAN_ID;
        private final Supplier<Integer> PARENT_SPAN_ID;
        private final Supplier<Integer> START_TIME;
        private final Supplier<Integer> COST_TIME;
        private final Supplier<Integer> END_TIME;
        private final Supplier<Integer> NAME;
        private final Supplier<Integer> KIND;
        private final Supplier<Integer> NORMALIZED_URL;
        private final Supplier<Integer> TAGS;
        private final Supplier<Integer> ATTRIBUTES;
        private final Supplier<Integer> CLAZZ;
        private final Supplier<Integer> METHOD;
        private final Supplier<Integer> STATUS;

        {
            APP_NAME = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN.APPNAME));
            INSTANCE_NAME = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN.INSTANCENAME));
            TRACE_ID = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN.TRACEID));
            SPAN_ID = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN.SPANID));
            PARENT_SPAN_ID = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN.PARENTSPANID));
            START_TIME = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN.STARTTIMEUS));
            COST_TIME = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN.COSTTIMEUS));
            END_TIME = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN.ENDTIMEUS));
            NAME = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN.NAME));
            KIND = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN.KIND));
            NORMALIZED_URL = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN.NORMALIZEDURL));
            TAGS = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN.TAGS));
            ATTRIBUTES = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN.ATTRIBUTES));
            CLAZZ = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN.CLAZZ));
            METHOD = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN.METHOD));
            STATUS = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN.STATUS));
        }

        public String getAppName(Record record) {
            return (String) record.get(APP_NAME.get());
        }

        public String getInstanceName(Record record) {
            return (String) record.get(INSTANCE_NAME.get());
        }

        public String getTraceId(Record record) {
            return (String) record.get(TRACE_ID.get());
        }

        public String getSpanId(Record record) {
            return (String) record.get(SPAN_ID.get());
        }

        public String getParentSpanId(Record record) {
            return (String) record.get(PARENT_SPAN_ID.get());
        }

        public long getStartTime(Record record) {
            return MicrosecondsUtils.toMicroseconds(record.get(START_TIME.get()));
        }

        public long getCostTime(Record record) {
            return (long) record.get(COST_TIME.get());
        }

        public long getEndTime(Record record) {
            return (long) record.get(END_TIME.get());
        }

        public String getName(Record record) {
            return (String) record.get(NAME.get());
        }

        public String getKind(Record record) {
            return (String) record.get(KIND.get());
        }

        public String getMethod(Record record) {
            return (String) record.get(METHOD.get());
        }

        public String getClazz(Record record) {
            return (String) record.get(CLAZZ.get());
        }

        public String getStatus(Record record) {
            return (String) record.get(STATUS.get());
        }

        public String getNormalizedUrl(Record record) {
            return (String) record.get(NORMALIZED_URL.get());
        }

        public String getTags(Record record) {
            return (String) record.get(TAGS.get());
        }

        public Object getAttributes(Record record) {
            return record.get(ATTRIBUTES.get());
        }
    }

    SpanSummaryTableRecordAccessor SUMMARY_TABLE_RECORD_ACCESSOR = new SpanSummaryTableRecordAccessor();
    SpanTableRecordAccessor TABLE_RECORD_ACCESSOR = new SpanTableRecordAccessor();
}
