package com.sbss.bithon.server.collector;

import com.sbss.bithon.agent.rpc.thrift.endpoint.ThriftServer;
import com.sbss.bithon.agent.rpc.thrift.service.event.IEventCollector;
import com.sbss.bithon.agent.rpc.thrift.service.metric.IMetricCollector;
import com.sbss.bithon.agent.rpc.thrift.service.setting.SettingService;
import com.sbss.bithon.agent.rpc.thrift.service.trace.ITraceCollector;
import org.apache.thrift.TBaseProcessor;
import org.apache.thrift.TMultiplexedProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 4:27 下午
 */
@Component
@ConditionalOnProperty(value = "collector-service.enabled", havingValue = "true", matchIfMissing = false)
public class CollectorServiceStarter {

    public CollectorServiceStarter(IMetricCollector.Iface metricCollector,
                                   SettingService.Iface settingService,
                                   ITraceCollector.Iface traceCollector,
                                   IEventCollector.Iface eventsCollector,
                                   CollectorServiceConfig config) {

        Map<Integer, TMultiplexedProcessor> processors = new HashMap<>();
        for (Map.Entry<String, Integer> entry : config.getPort().entrySet()) {
            String service = entry.getKey();
            Integer value = entry.getValue();

            TBaseProcessor processor = null;
            switch (service) {
                case "metric":
                    processor = new IMetricCollector.Processor<>(metricCollector);
                    break;

                case "event":
                    processor = new IEventCollector.Processor<>(eventsCollector);
                    break;

                case "trace":
                    processor = new ITraceCollector.Processor<>(traceCollector);
                    break;

                case "setting":
                    processor = new SettingService.Processor<>(settingService);
                    break;

                default:
                    break;
            }
            if (processor != null) {
                processors.computeIfAbsent(value, key -> new TMultiplexedProcessor())
                    .registerProcessor(service, processor);
            }
        }

        processors.forEach((port, processor) -> {
            new ThriftServer().start(processor, port);
        });
    }
}
