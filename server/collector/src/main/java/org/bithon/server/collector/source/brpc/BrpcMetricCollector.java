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

package org.bithon.server.collector.source.brpc;


import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.extern.slf4j.Slf4j;
import org.bithon.agent.rpc.brpc.BrpcMessageHeader;
import org.bithon.agent.rpc.brpc.metrics.BrpcGenericMeasurement;
import org.bithon.agent.rpc.brpc.metrics.BrpcGenericMetricMessage;
import org.bithon.agent.rpc.brpc.metrics.BrpcGenericMetricMessageV2;
import org.bithon.agent.rpc.brpc.metrics.BrpcJvmMetricMessage;
import org.bithon.agent.rpc.brpc.metrics.IMetricCollector;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.ReflectionUtils;
import org.bithon.server.collector.source.http.MetricHttpCollector;
import org.bithon.server.pipeline.metrics.IMetricProcessor;
import org.bithon.server.pipeline.metrics.MetricMessage;
import org.bithon.server.pipeline.metrics.SchemaMetricMessage;
import org.bithon.server.pipeline.metrics.receiver.IMetricReceiver;
import org.bithon.server.storage.datasource.DefaultSchema;
import org.bithon.server.storage.datasource.TimestampSpec;
import org.bithon.server.storage.datasource.column.IColumn;
import org.bithon.server.storage.datasource.column.StringColumn;
import org.bithon.server.storage.datasource.column.aggregatable.last.AggregateLongLastColumn;
import org.bithon.server.storage.datasource.column.aggregatable.max.AggregateLongMaxColumn;
import org.bithon.server.storage.datasource.column.aggregatable.min.AggregateLongMinColumn;
import org.bithon.server.storage.datasource.column.aggregatable.sum.AggregateLongSumColumn;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 2:37 下午
 */
@Slf4j
@JsonTypeName("brpc")
public class BrpcMetricCollector implements IMetricCollector, IMetricReceiver {

    private final int port;
    private final ApplicationContext applicationContext;
    private IMetricProcessor processor;
    private BrpcCollectorServer.ServiceGroup serviceGroup;

    public BrpcMetricCollector(@JacksonInject(useInput = OptBoolean.FALSE) Environment environment,
                               @JacksonInject(useInput = OptBoolean.FALSE) ApplicationContext applicationContext) {
        BrpcCollectorConfig config = Binder.get(environment).bind("bithon.receivers.metrics.brpc", BrpcCollectorConfig.class).get();
        Preconditions.checkIfTrue(config.isEnabled(), "The brpc collector is configured as DISABLED.");
        Preconditions.checkNotNull(config.getPort(), "The port for the metrics collector is not configured.");
        Preconditions.checkIfTrue(config.getPort() > 1000 && config.getPort() < 65535, "The port for the event collector must be in the range of (1000, 65535).");

        this.port = config.getPort();
        this.applicationContext = applicationContext;
    }

    @Override
    public void start() {
        serviceGroup = this.applicationContext.getBean(BrpcCollectorServer.class)
                                              .addService("metrics", this, port);
    }

    @Override
    public void registerProcessor(IMetricProcessor processor) {
        this.processor = processor;

        try {
            this.applicationContext.getBean(MetricHttpCollector.class).setProcessor(processor);
        } catch (NoSuchBeanDefinitionException ignored) {
        }
    }

    @Override
    public void stop() {
        serviceGroup.stop("metrics");
    }

    private final IColumn appName = new StringColumn("appName", "appName");
    private final IColumn instanceName = new StringColumn("instanceName", "instanceName");

    @Override
    public void sendJvm(BrpcMessageHeader header, List<BrpcJvmMetricMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }

        this.processor.process("jvm-metrics",
                               SchemaMetricMessage.builder()
                                                  .metrics(messages.stream().map((m) -> toMetricMessage(header, m)).collect(Collectors.toList()))
                                                  .build());
    }

    @Override
    public void sendGenericMetrics(BrpcMessageHeader header, BrpcGenericMetricMessage message) {
        if (processor == null) {
            return;
        }

        List<IColumn> dimensionSpecs = new ArrayList<>();
        dimensionSpecs.add(appName);
        dimensionSpecs.add(instanceName);
        dimensionSpecs.addAll(message.getSchema()
                                     .getDimensionsSpecList()
                                     .stream()
                                     .map(dimSpec -> new StringColumn(dimSpec.getName(), dimSpec.getName()))
                                     .collect(Collectors.toList()));

        DefaultSchema schema = new DefaultSchema(message.getSchema().getName(),
                                                 message.getSchema().getName(),
                                                 new TimestampSpec("timestamp", "auto", null),
                                                 dimensionSpecs,
                                                 message.getSchema().getMetricsSpecList().stream().map(metricSpec -> {
                                                           if ("longMax".equals(metricSpec.getType())) {
                                                               return new AggregateLongMaxColumn(metricSpec.getName(), metricSpec.getName());
                                                           }
                                                           if ("longMin".equals(metricSpec.getType())) {
                                                               return new AggregateLongMinColumn(metricSpec.getName(), metricSpec.getName());
                                                           }
                                                           if ("longSum".equals(metricSpec.getType())) {
                                                               return new AggregateLongSumColumn(metricSpec.getName(), metricSpec.getName());
                                                           }
                                                           if ("longLast".equals(metricSpec.getType())) {
                                                               return new AggregateLongLastColumn(metricSpec.getName(), metricSpec.getName());
                                                           }

                                                           return null;
                                                       }).collect(Collectors.toList()));

        Iterator<BrpcGenericMeasurement> iterator = message.getMeasurementList().iterator();
        SchemaMetricMessage schemaMetricMessage = new SchemaMetricMessage();
        schemaMetricMessage.setSchema(schema);
        schemaMetricMessage.setMetrics(message.getMeasurementList().stream().map((m) -> {
            MetricMessage metricMessage = new MetricMessage();
            BrpcGenericMeasurement measurement = iterator.next();

            int i = 0;
            for (String dimension : measurement.getDimensionList()) {
                IColumn dimensionSpec = schema.getDimensionsSpec().get(i++);
                metricMessage.put(dimensionSpec.getName(), dimension);
            }

            i = 0;
            for (long value : measurement.getMetricList()) {
                IColumn metricSpec = schema.getMetricsSpec().get(i++);
                metricMessage.put(metricSpec.getName(), value);
            }

            metricMessage.put("interval", message.getInterval());
            metricMessage.put("timestamp", message.getTimestamp());
            metricMessage.put("instance", header.getInstanceName());
            ReflectionUtils.getFields(header, metricMessage);
            return metricMessage;
        }).collect(Collectors.toList()));

        processor.process(message.getSchema().getName(), schemaMetricMessage);
    }

    @Override
    public void sendGenericMetricsV2(BrpcMessageHeader header, BrpcGenericMetricMessageV2 message) {
        List<IInputRow> messages = message.getMeasurementList().stream().map((measurement) -> {
            MetricMessage metricMessage = new MetricMessage();
            int i = 0;
            for (String dimension : measurement.getDimensionList()) {
                String dimensionSpec = message.getSchema().getDimensionsSpec(i++);
                metricMessage.put(dimensionSpec, dimension);
            }

            i = 0;
            for (long metric : measurement.getMetricList()) {
                String metricSpec = message.getSchema().getMetricsSpec(i++);
                metricMessage.put(metricSpec, metric);
            }

            metricMessage.put("interval", message.getInterval());
            metricMessage.put("timestamp", message.getTimestamp());
            ReflectionUtils.getFields(header, metricMessage);
            return metricMessage;
        }).collect(Collectors.toList());

        this.processor.process(message.getSchema().getName(),
                               SchemaMetricMessage.builder()
                                                  .metrics(messages)
                                                  .build());
    }

    private MetricMessage toMetricMessage(BrpcMessageHeader header, Object message) {
        MetricMessage metricMessage = new MetricMessage();
        ReflectionUtils.getFields(header, metricMessage);
        ReflectionUtils.getFields(message, metricMessage);

        // adaptor
        // protobuf turns the name 'count4xx' in .proto file to 'count4Xx'
        // we have to convert it back to make it compatible with existing name style
        Object count4xx = metricMessage.remove("count4Xx");
        if (count4xx != null) {
            metricMessage.put("count4xx", count4xx);
        }
        Object count5xx = metricMessage.remove("count5Xx");
        if (count5xx != null) {
            metricMessage.put("count5xx", count5xx);
        }

        return metricMessage;
    }
}
