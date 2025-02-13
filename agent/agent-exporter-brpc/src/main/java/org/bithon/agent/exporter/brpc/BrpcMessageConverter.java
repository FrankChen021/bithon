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

package org.bithon.agent.exporter.brpc;

import org.bithon.agent.instrumentation.expt.AgentException;
import org.bithon.agent.observability.event.EventMessage;
import org.bithon.agent.observability.exporter.IMessageConverter;
import org.bithon.agent.observability.metric.domain.jvm.JvmMetrics;
import org.bithon.agent.observability.metric.domain.sql.SQLMetrics;
import org.bithon.agent.observability.metric.domain.sql.SQLStatementMetrics;
import org.bithon.agent.observability.metric.model.IMeasurement;
import org.bithon.agent.observability.metric.model.schema.Schema;
import org.bithon.agent.observability.metric.model.schema.Schema2;
import org.bithon.agent.observability.metric.model.schema.Schema3;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.rpc.brpc.event.BrpcEventMessage;
import org.bithon.agent.rpc.brpc.metrics.BrpcGenericDimensionSpec;
import org.bithon.agent.rpc.brpc.metrics.BrpcGenericMeasurement;
import org.bithon.agent.rpc.brpc.metrics.BrpcGenericMeasurementV3;
import org.bithon.agent.rpc.brpc.metrics.BrpcGenericMetricMessage;
import org.bithon.agent.rpc.brpc.metrics.BrpcGenericMetricMessageV2;
import org.bithon.agent.rpc.brpc.metrics.BrpcGenericMetricMessageV3;
import org.bithon.agent.rpc.brpc.metrics.BrpcGenericMetricSchema;
import org.bithon.agent.rpc.brpc.metrics.BrpcGenericMetricSchemaV2;
import org.bithon.agent.rpc.brpc.metrics.BrpcGenericMetricSchemaV3;
import org.bithon.agent.rpc.brpc.metrics.BrpcGenericMetricSpec;
import org.bithon.agent.rpc.brpc.metrics.BrpcJvmMetricMessage;
import org.bithon.agent.rpc.brpc.metrics.FieldType;
import org.bithon.agent.rpc.brpc.tracing.BrpcTraceSpanMessage;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.bithon.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.shaded.com.fasterxml.jackson.databind.SerializationFeature;
import org.bithon.shaded.com.google.protobuf.Any;
import org.bithon.shaded.com.google.protobuf.DoubleValue;
import org.bithon.shaded.com.google.protobuf.Int64Value;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/6/27 20:13
 */
public class BrpcMessageConverter implements IMessageConverter {

    private static final ILogAdaptor logger = LoggerFactory.getLogger(BrpcMessageConverter.class);

    private final ObjectMapper objectMapper;

    public BrpcMessageConverter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    @Override
    public Object from(long timestamp, int interval, List<String> dimensions, SQLMetrics metrics) {
        return null;
    }

    @Override
    public Object from(long timestamp, int interval, JvmMetrics metrics) {
        BrpcJvmMetricMessage.Builder builder = BrpcJvmMetricMessage.newBuilder();
        builder.setInterval(interval);
        builder.setTimestamp(timestamp);
        builder.setInstanceStartTime(metrics.startTime);
        builder.setInstanceUpTime(metrics.upTime);
        builder.setProcessors(metrics.cpu.processorNumber);
        builder.setProcessCpuLoad(metrics.cpu.processCpuLoad);
        builder.setProcessCpuTime(metrics.cpu.processCpuTime);
        builder.setSystemLoadAvg(metrics.cpu.avgSystemLoad);
        builder.setTotalMemBytes(metrics.memory.allocatedBytes);
        builder.setFreeMemBytes(metrics.memory.freeBytes);
        builder.setHeapMax(metrics.heap.max);
        builder.setHeapInit(metrics.heap.init);
        builder.setHeapCommitted(metrics.heap.committed);
        builder.setHeapUsed(metrics.heap.used);
        builder.setNonHeapMax(metrics.nonHeap.max);
        builder.setNonHeapInit(metrics.nonHeap.init);
        builder.setNonHeapCommitted(metrics.nonHeap.committed);
        builder.setNonHeapUsed(metrics.nonHeap.used);
        builder.setPeakThreads(metrics.thread.peakActiveCount);
        builder.setActiveThreads(metrics.thread.activeThreadsCount);
        builder.setDaemonThreads(metrics.thread.activeDaemonCount);
        builder.setTotalThreads(metrics.thread.totalCreatedCount);
        builder.setClassLoaded(metrics.clazz.currentLoadedClasses);
        builder.setClassUnloaded(metrics.clazz.totalUnloadedClasses);
        builder.setMetaspaceCommitted(metrics.metaspace.committed);
        builder.setMetaspaceUsed(metrics.metaspace.used);
        builder.setMetaspaceInit(metrics.metaspace.init);
        builder.setMetaspaceMax(metrics.metaspace.max);
        builder.setDirectMax(metrics.directMemory.max);
        builder.setDirectUsed(metrics.directMemory.used);
        return builder.build();
    }

    @Override
    public Object from(long timestamp, int interval, SQLStatementMetrics metrics) {
        return null;
    }

    @Override
    public Object from(ITraceSpan span) {
        BrpcTraceSpanMessage.Builder builder = BrpcTraceSpanMessage.newBuilder()
                                                                   .setTraceId(span.traceId())
                                                                   .setSpanId(span.spanId())
                                                                   .setStartTime(span.startTime())
                                                                   .setEndTime(span.endTime())
                                                                   .setKind(span.kind().toString())
                                                                   .setName(span.component())
                                                                   .setClazz(span.clazz())
                                                                   .setMethod(span.method());

        // The 'putllAllTags' on the Builder internally checks the NULL of each k-v pair.
        // To avoid unexpected exception, we do the check by ourselves so that we know which tag has the NULL value.
        for (Map.Entry<String, String> entry : span.tags().entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            if (v != null) {
                builder.putTags(k, v);
            } else {
                // An exception is construct to parse to the error method,
                // so tha that the logback/log4j plugin can record the exception into the exception log.
                logger.error(StringUtils.format("Unexpected exception",
                                                new AgentException("Value of tag [%s] is null", k)));
            }
        }

        if (span.parentApplication() != null) {
            builder.setParentAppName(span.parentApplication());
        }
        if (span.parentSpanId() != null) {
            builder.setParentSpanId(span.parentSpanId());
        }
        return builder.build();
    }

    @Override
    public Object from(EventMessage event) {
        String jsonArgs;
        try {
            jsonArgs = objectMapper.writeValueAsString(event.getArgs());
        } catch (JsonProcessingException ignored) {
            jsonArgs = "{}";
        }
        return BrpcEventMessage.newBuilder()
                               .setTimestamp(System.currentTimeMillis())
                               .setEventType(event.getMessageType())
                               .setJsonArguments(jsonArgs)
                               .build();
    }

    @Override
    public Object from(Map<String, String> log) {
        return null;
    }

    @Override
    public Object from(Schema schema,
                       Collection<IMeasurement> measurementList,
                       long timestamp,
                       int interval) {
        BrpcGenericMetricSchema.Builder schemaBuilder = BrpcGenericMetricSchema.newBuilder()
                                                                               .setName(schema.getName());
        schema.getDimensionsSpec()
              .forEach(dimensionSpec -> schemaBuilder.addDimensionsSpec(BrpcGenericDimensionSpec.newBuilder()
                                                                                                .setName(dimensionSpec.getName())
                                                                                                .setType(dimensionSpec.getType())
                                                                                                .build()));
        schema.getMetricsSpec().forEach(metricSpec -> schemaBuilder.addMetricsSpec(BrpcGenericMetricSpec.newBuilder()
                                                                                                        .setName(
                                                                                                            metricSpec.getName())
                                                                                                        .setType(
                                                                                                            metricSpec.getType())
                                                                                                        .build()));

        BrpcGenericMetricMessage.Builder messageBuilder = BrpcGenericMetricMessage.newBuilder();
        messageBuilder.setSchema(schemaBuilder.build());
        messageBuilder.setInterval(interval);
        messageBuilder.setTimestamp(timestamp);

        measurementList.forEach(metricSet -> {
            try {
                BrpcGenericMeasurement.Builder measurement = BrpcGenericMeasurement.newBuilder();
                // although dimensions are defined as List<String>
                // it could also store an object,
                // we use Object.toString here to get the right value
                for (String dimension : metricSet.getDimensions().values()) {
                    measurement.addDimension(dimension);
                }
                for (int i = 0, size = metricSet.getMetricCount(); i < size; i++) {
                    measurement.addMetric(metricSet.getMetricValue(i));
                }
                messageBuilder.addMeasurement(measurement.build());
            } catch (RuntimeException e) {
                logger.error(StringUtils.format("Invalid measurement: %s, dimensions=%s", schema.getName(), metricSet.getDimensions()), e);
            }
        });

        return messageBuilder.build();
    }

    @Override
    public Object from(Schema2 schema, Collection<IMeasurement> measurementList, long timestamp, int interval) {
        BrpcGenericMetricSchemaV2.Builder schemaBuilder = BrpcGenericMetricSchemaV2.newBuilder()
                                                                                   .setName(schema.getName());
        schema.getDimensionsSpec().forEach(schemaBuilder::addDimensionsSpec);
        schema.getMetricsSpec().forEach(schemaBuilder::addMetricsSpec);

        BrpcGenericMetricMessageV2.Builder messageBuilder = BrpcGenericMetricMessageV2.newBuilder();
        messageBuilder.setSchema(schemaBuilder.build());
        messageBuilder.setInterval(interval);
        messageBuilder.setTimestamp(timestamp);

        measurementList.forEach(measurement -> {
            try {
                BrpcGenericMeasurement.Builder measurementBuilder = BrpcGenericMeasurement.newBuilder();
                // although dimensions are defined as List<String>
                // it could also store Object,
                // we use Object.toString here to get the right value
                for (String dimension : measurement.getDimensions().values()) {
                    measurementBuilder.addDimension(dimension == null ? "" : dimension);
                }
                for (int i = 0, size = measurement.getMetricCount(); i < size; i++) {
                    measurementBuilder.addMetric(measurement.getMetricValue(i));
                }
                messageBuilder.addMeasurement(measurementBuilder.build());
            } catch (RuntimeException ignored) {
                // ignore invalid metric values
            }
        });

        return messageBuilder.build();
    }

    @Override
    public Object from(Schema3 schema, List<Object[]> measurementList, long timestamp, int interval) {
        BrpcGenericMetricSchemaV3.Builder schemaBuilder = BrpcGenericMetricSchemaV3.newBuilder()
                                                                                   .setName(schema.getName());
        schema.getFields()
              .stream()
              .map((fieldSpec -> BrpcGenericMetricSchemaV3.FieldSpec
                  .newBuilder()
                  .setName(fieldSpec.getName())
                  .setType(FieldType.DOUBLE).build()))
              .forEach((schemaBuilder::addFieldSpec));

        BrpcGenericMetricMessageV3.Builder messageBuilder = BrpcGenericMetricMessageV3.newBuilder();
        messageBuilder.setSchema(schemaBuilder.build());
        messageBuilder.setInterval(interval);
        messageBuilder.setTimestamp(timestamp);

        measurementList.forEach(measurement -> {
            try {
                BrpcGenericMeasurementV3.Builder measurementBuilder = BrpcGenericMeasurementV3.newBuilder();

                for (Object v : measurement) {
                    if (v instanceof Long) {
                        measurementBuilder.addValue(Any.pack(Int64Value.of((Long) v)));
                    } else if (v instanceof Integer) {
                        measurementBuilder.addValue(Any.pack(Int64Value.of((Integer) v)));
                    } else if (v instanceof Double) {
                        measurementBuilder.addValue(Any.pack(DoubleValue.of((Double) v)));
                    } else if (v instanceof Float) {
                        measurementBuilder.addValue(Any.pack(DoubleValue.of((Float) v)));
                    } else {
                        throw new RuntimeException("Not supported type " + v.getClass().getName());
                    }
                }
                messageBuilder.setMeasurement(measurementBuilder.build());
            } catch (RuntimeException ignored) {
                // ignore invalid metric values
            }
        });

        return messageBuilder.build();
    }
}
