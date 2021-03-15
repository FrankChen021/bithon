package com.sbss.bithon.server.collector.protocol;

import com.sbss.bithon.agent.rpc.thrift.service.MessageHeader;
import com.sbss.bithon.agent.rpc.thrift.service.trace.ITraceCollector;
import com.sbss.bithon.agent.rpc.thrift.service.trace.TraceSpanMessage;
import com.sbss.bithon.server.tracing.collector.TraceMessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/23 11:19 下午
 */
@Slf4j
@Service
public class TraceCollectorThriftImpl implements ITraceCollector.Iface {

    private final TraceMessageHandler handler;

    public TraceCollectorThriftImpl(TraceMessageHandler handler) {
        this.handler = handler;
    }

    @Override
    public void sendTrace(MessageHeader header, List<TraceSpanMessage> spans) {
        log.info("Receiving trace message:{}", spans);
        handler.submit(header, spans);
    }
}
