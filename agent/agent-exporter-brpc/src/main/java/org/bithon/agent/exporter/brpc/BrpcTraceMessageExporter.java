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

import org.bithon.agent.observability.context.AppInstance;
import org.bithon.agent.observability.exporter.IMessageExporter;
import org.bithon.agent.observability.exporter.config.ExporterConfig;
import org.bithon.agent.rpc.brpc.ApplicationType;
import org.bithon.agent.rpc.brpc.BrpcMessageHeader;
import org.bithon.agent.rpc.brpc.tracing.BrpcTraceSpanMessage;
import org.bithon.agent.rpc.brpc.tracing.ITraceCollector;
import org.bithon.component.brpc.IServiceController;
import org.bithon.component.brpc.channel.BrpcClient;
import org.bithon.component.brpc.channel.BrpcClientBuilder;
import org.bithon.component.brpc.channel.IBrpcChannel;
import org.bithon.component.brpc.endpoint.EndPoint;
import org.bithon.component.brpc.endpoint.RoundRobinEndPointProvider;
import org.bithon.component.brpc.exception.CallerSideException;
import org.bithon.component.brpc.exception.ServiceInvocationException;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/6/27 20:14
 */
public class BrpcTraceMessageExporter implements IMessageExporter {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(BrpcTraceMessageExporter.class);

    private final ExporterConfig exporterConfig;
    private final BrpcClient brpcClient;
    private ITraceCollector traceCollector;
    private BrpcMessageHeader header;

    public BrpcTraceMessageExporter(ExporterConfig exporterConfig) {

        List<EndPoint> endpoints = Stream.of(exporterConfig.getServers().split(",")).map(hostAndPort -> {
            String[] parts = hostAndPort.split(":");
            return new EndPoint(parts[0], Integer.parseInt(parts[1]));
        }).collect(Collectors.toList());
        this.brpcClient = BrpcClientBuilder.builder()
                                           .clientId("trace")
                                           .server(new RoundRobinEndPointProvider(endpoints))
                                           .maxRetry(3)
                                           .retryBackOff(Duration.ofMillis(200))
                                           .connectionTimeout(Duration.ofMillis(exporterConfig.getClient().getConnectionTimeout()))
                                           .lowMaterMark(exporterConfig.getClient().getLowWaterMark().intValue())
                                           .highMaterMark(exporterConfig.getClient().getHighWaterMark().intValue())
                                           .build();

        this.exporterConfig = exporterConfig;

        AppInstance appInstance = AppInstance.getInstance();
        this.header = BrpcMessageHeader.newBuilder()
                                       .setAppName(appInstance.getQualifiedName())
                                       .setEnv(appInstance.getEnv())
                                       .setInstanceName(appInstance.getInstanceName())
                                       .setAppType(ApplicationType.JAVA)
                                       .build();
        appInstance.addListener(port -> this.header = BrpcMessageHeader.newBuilder()
                                                                       .setAppName(appInstance.getQualifiedName())
                                                                       .setEnv(appInstance.getEnv())
                                                                       .setInstanceName(appInstance.getInstanceName())
                                                                       .setAppType(ApplicationType.JAVA)
                                                                       .build());
    }

    @Override
    public void export(Object message) {
        if (this.traceCollector == null) {
            try {
                this.traceCollector = this.brpcClient.getRemoteService(ITraceCollector.class);
            } catch (ServiceInvocationException e) {
                LOG.warn("Unable to get remote ITraceCollector service: {}", e.getMessage());
                return;
            }
        }

        if (!(message instanceof List)) {
            return;
        }
        if (((List<?>) message).isEmpty()) {
            return;
        }

        IBrpcChannel channel = ((IServiceController) traceCollector).getChannel();
        if (channel.getConnectionLifeTime() > exporterConfig.getClient().getConnectionLifeTime()) {
            LOG.info("Disconnect trace-channel for client-side load balancing...");
            try {
                channel.disconnect();
            } catch (Exception ignored) {
            }
        }

        boolean isDebugOn = this.exporterConfig.getMessageDebug()
                                               .getOrDefault(BrpcTraceSpanMessage.class.getName(), false);
        if (isDebugOn) {
            LOG.info("[Debugging] Sending Tracing Messages: {}", message);
        }

        try {
            //noinspection unchecked
            this.traceCollector.sendTrace(this.header,
                                          (List<BrpcTraceSpanMessage>) message);
        } catch (CallerSideException e) {
            //suppress client exception
            LOG.error("Failed to send tracing: {}", e.getMessage());
        }
    }

    @Override
    public void close() {
        this.brpcClient.close();
    }
}
