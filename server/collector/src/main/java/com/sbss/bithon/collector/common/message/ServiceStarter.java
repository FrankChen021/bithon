package com.sbss.bithon.collector.common.message;

import com.sbss.bithon.agent.rpc.thrift.endpoint.ThriftServer;
import com.sbss.bithon.agent.rpc.thrift.service.event.IEventCollector;
import com.sbss.bithon.agent.rpc.thrift.service.metric.IMetricCollector;
import com.sbss.bithon.agent.rpc.thrift.service.setting.SettingService;
import com.sbss.bithon.agent.rpc.thrift.service.trace.TraceCollectorService;
import org.springframework.stereotype.Component;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 4:27 下午
 */
@Component
public class ServiceStarter {

    public ServiceStarter(IMetricCollector.Iface metricCollector,
                          SettingService.Iface settingService,
                          TraceCollectorService.Iface traceCollector,
                          IEventCollector.Iface eventsCollector,
                          ServiceConfig config) {
        new ThriftServer().start(new IMetricCollector.Processor<>(metricCollector),
                                 config.getPort().get("metrics"));

        new ThriftServer().start(new SettingService.Processor<>(settingService),
                                 config.getPort().get("setting"));

        new ThriftServer().start(new TraceCollectorService.Processor<>(traceCollector),
                                 config.getPort().get("trace"));

        new ThriftServer().start(new IEventCollector.Processor<>(eventsCollector),
                                 config.getPort().get("events"));

        //TODO: support multiple services on ONE port
    }
}
