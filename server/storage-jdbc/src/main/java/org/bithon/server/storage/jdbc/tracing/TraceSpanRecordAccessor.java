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

package org.bithon.server.storage.jdbc.tracing;

import org.bithon.component.commons.utils.SupplierUtils;
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.jooq.Record;

import java.util.function.Supplier;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/5/7 16:41
 */
class TraceSpanRecordAccessor {
    private static final Supplier<Integer> APP_NAME;
    private static final Supplier<Integer> INSTANCE_NAME;
    private static final Supplier<Integer> TRACE_ID;
    private static final Supplier<Integer> SPAN_ID;
    private static final Supplier<Integer> PARENT_SPAN_ID;
    private static final Supplier<Integer> START_TIME;
    private static final Supplier<Integer> COST_TIME;
    private static final Supplier<Integer> END_TIME;
    private static final Supplier<Integer> NAME;
    private static final Supplier<Integer> KIND;
    private static final Supplier<Integer> NORMALIZED_URL;
    private static final Supplier<Integer> TAGS;
    private static final Supplier<Integer> ATTRIBUTES;
    private static final Supplier<Integer> CLAZZ;
    private static final Supplier<Integer> METHOD;
    private static final Supplier<Integer> STATUS;

    static {
        APP_NAME = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN_SUMMARY.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN_SUMMARY.APPNAME));
        INSTANCE_NAME = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN_SUMMARY.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN_SUMMARY.INSTANCENAME));
        TRACE_ID = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN_SUMMARY.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN_SUMMARY.TRACEID));
        SPAN_ID = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN_SUMMARY.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN_SUMMARY.SPANID));
        PARENT_SPAN_ID = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN_SUMMARY.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN_SUMMARY.PARENTSPANID));
        START_TIME = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN_SUMMARY.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN_SUMMARY.STARTTIMEUS));
        COST_TIME = SupplierUtils.cachedWithoutLock(() -> Tables.BITHON_TRACE_SPAN_SUMMARY.fieldsRow().indexOf(Tables.BITHON_TRACE_SPAN_SUMMARY.COSTTIMEMS));
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

    public static String getAppName(Record record) {
        return (String) record.get(APP_NAME.get());
    }

    public static String getInstanceName(Record record) {
        return (String) record.get(INSTANCE_NAME.get());
    }

    public static String getTraceId(Record record) {
        return (String) record.get(TRACE_ID.get());
    }

    public static String getSpanId(Record record) {
        return (String) record.get(SPAN_ID.get());
    }

    public static String getParentSpanId(Record record) {
        return (String) record.get(PARENT_SPAN_ID.get());
    }

    public static long getStartTime(Record record) {
        return (long) record.get(START_TIME.get());
    }

    public static long getCostTime(Record record) {
        return (long) record.get(COST_TIME.get());
    }

    public static long getEndTime(Record record) {
        return (long) record.get(END_TIME.get());
    }

    public static String getName(Record record) {
        return (String) record.get(NAME.get());
    }

    public static String getKind(Record record) {
        return (String) record.get(KIND.get());
    }

    public static String getMethod(Record record) {
        return (String) record.get(METHOD.get());
    }

    public static String getClazz(Record record) {
        return (String) record.get(CLAZZ.get());
    }

    public static String getStatus(Record record) {
        return (String) record.get(STATUS.get());
    }

    public static String getNormalizedUrl(Record record) {
        return (String) record.get(NORMALIZED_URL.get());
    }

    public static String getTags(Record record) {
        return (String) record.get(TAGS.get());
    }

    public static Object getAttributes(Record record) {
        return record.get(ATTRIBUTES.get());
    }
}
