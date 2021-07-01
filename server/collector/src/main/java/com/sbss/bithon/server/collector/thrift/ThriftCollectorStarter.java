/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.server.collector.thrift;

import com.sbss.bithon.agent.rpc.thrift.service.event.IEventCollector;
import com.sbss.bithon.agent.rpc.thrift.service.metric.IMetricCollector;
import com.sbss.bithon.agent.rpc.thrift.service.setting.SettingService;
import com.sbss.bithon.agent.rpc.thrift.service.trace.ITraceCollector;
import com.sbss.bithon.server.collector.sink.IMessageSink;
import com.sbss.bithon.server.setting.AgentSettingService;
import com.sbss.bithon.server.setting.SettingServiceThriftImpl;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TBaseProcessor;
import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.TProcessorFactory;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.layered.TFramedTransport;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 4:27 下午
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "collector-thrift.enabled", havingValue = "true", matchIfMissing = false)
public class ThriftCollectorStarter implements SmartLifecycle, ApplicationContextAware {

    private final List<TThreadedSelectorServer> thriftServers = new ArrayList<>();
    private ApplicationContext applicationContext;

    @Getter
    static class ServiceGroup {
        private final List<String> services = new ArrayList<>();
        private final TMultiplexedProcessor processor = new TMultiplexedProcessor();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void start() {
        ThriftCollectorConfig config = applicationContext.getBean(ThriftCollectorConfig.class);
        Map<Integer, ServiceGroup> serviceGroups = new HashMap<>();

        //
        // group services by their listening ports
        //
        for (Map.Entry<String, Integer> entry : config.getPort().entrySet()) {
            String service = entry.getKey();
            Integer port = entry.getValue();

            TBaseProcessor<?> processor = null;
            switch (service) {
                case "metric":
                    processor = new IMetricCollector.Processor<>(new ThriftMetricCollector(applicationContext.getBean(
                        "metricSink",
                        IMessageSink.class)));
                    break;

                case "event":
                    processor = new IEventCollector.Processor<>(new ThriftEventCollector(applicationContext.getBean(
                        "eventSink",
                        IMessageSink.class)));
                    break;

                case "tracing":
                    processor = new ITraceCollector.Processor<>(new ThriftTraceCollector(applicationContext.getBean(
                        "traceSink",
                        IMessageSink.class)));
                    break;

                case "setting":
                    processor = new SettingService.Processor<>(new SettingServiceThriftImpl(applicationContext.getBean(
                        AgentSettingService.class)));
                    break;

                default:
                    break;
            }
            if (processor != null) {
                ServiceGroup serviceGroup = serviceGroups.computeIfAbsent(port, key -> new ServiceGroup());
                serviceGroup.getServices().add(service);
                serviceGroup.getProcessor().registerProcessor(service, processor);
            }
        }

        serviceGroups.forEach((port, serviceGroup) -> {
            final String serviceName = String.join(",", serviceGroup.getServices());
            try {
                TThreadedSelectorServer.Args args = new TThreadedSelectorServer.Args(new TNonblockingServerSocket(port));
                args.processorFactory(new TProcessorFactory(serviceGroup.getProcessor()));
                args.transportFactory(new TFramedTransport.Factory());
                args.protocolFactory(new TCompactProtocol.Factory());
                // enlarge the following IO threads only when the traffic is very large
                args.selectorThreads(Runtime.getRuntime().availableProcessors());

                TThreadedSelectorServer thriftServer = new TThreadedSelectorServer(args);
                if (thriftServer.isServing()) {
                    throw new RuntimeException(String.format(
                        "Failed to start thrift server on port [%d]: The port is already in used",
                        port));
                }

                thriftServers.add(thriftServer);
                new Thread(() -> {
                    log.info("Starting thrift server[{}] on port {}...", serviceName, port);
                    thriftServer.serve();
                    log.info("Thrift server[{}] stopped", serviceName);
                }, "thrift-server-" + serviceName).start();
            } catch (TTransportException e) {
                throw new RuntimeException(String.format("Failed to start thrift server[%s] on port [%d]: %s",
                                                         serviceName,
                                                         port,
                                                         e.getMessage()), e);
            }
        });
    }

    @Override
    public void stop() {
        for (TThreadedSelectorServer server : thriftServers) {
            server.stop();
        }
    }

    @Override
    public boolean isRunning() {
        return thriftServers.stream().anyMatch(TServer::isServing);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
