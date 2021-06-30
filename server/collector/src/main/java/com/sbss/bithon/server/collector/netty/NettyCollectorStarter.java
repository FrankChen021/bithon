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

package com.sbss.bithon.server.collector.netty;

import cn.bithon.rpc.IService;
import cn.bithon.rpc.channel.ServerChannel;
import cn.bithon.rpc.services.IMetricCollector;
import cn.bithon.rpc.services.ITraceCollector;
import com.sbss.bithon.server.collector.sink.IMessageSink;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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
@ConditionalOnProperty(value = "collector-netty.enabled", havingValue = "true", matchIfMissing = false)
public class NettyCollectorStarter implements SmartLifecycle, ApplicationContextAware {

    private final List<ServerChannel> servers = new ArrayList<>();
    private ApplicationContext applicationContext;
    private boolean isRunning;

    @Getter
    @AllArgsConstructor
    static class ServiceImpl {
        private final Class<? extends IService> clazz;
        private final IService impl;
    }

    @Getter
    static class ServiceGroup {
        private final List<ServiceImpl> services = new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void start() {
        NettyCollectorConfig config = applicationContext.getBean(NettyCollectorConfig.class);
        Map<Integer, ServiceGroup> serviceGroups = new HashMap<>();

        //
        // group services by their listening ports
        //
        for (Map.Entry<String, Integer> entry : config.getPort().entrySet()) {
            String service = entry.getKey();
            Integer port = entry.getValue();

            Class<? extends IService> clazz = null;
            IService processor = null;
            switch (service) {
                case "metric":
                    clazz = IMetricCollector.class;
                    processor = new NettyMetricCollector(applicationContext.getBean("metricSink", IMessageSink.class));
                    break;

                case "event":
                    break;

                case "tracing":
                    clazz = ITraceCollector.class;
                    processor = new NettyTraceCollector(applicationContext.getBean("traceSink", IMessageSink.class));
                    break;

                case "setting":
                    //processor = new SettingService.Processor<>(applicationContext.getBean(SettingService.Iface.class));
                    break;

                default:
                    break;
            }
            if (processor != null) {
                ServiceGroup serviceGroup = serviceGroups.computeIfAbsent(port, key -> new ServiceGroup());
                serviceGroup.getServices().add(new ServiceImpl(clazz, processor));
            }
        }

        serviceGroups.forEach((port, serviceGroup) -> {
            ServerChannel channel = new ServerChannel();
            serviceGroup.getServices().forEach((service) -> channel.bindService(service.getClazz(), service.getImpl()));
            channel.start(port);
        });

        isRunning = true;
    }

    @Override
    public void stop() {
        servers.forEach(ServerChannel::close);
        isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
