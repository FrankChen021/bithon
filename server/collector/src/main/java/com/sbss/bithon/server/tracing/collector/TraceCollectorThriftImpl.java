package com.sbss.bithon.server.tracing.collector;

import com.sbss.bithon.agent.rpc.thrift.service.trace.ITraceCollector;
import com.sbss.bithon.agent.rpc.thrift.service.trace.TraceMessage;
import com.sbss.bithon.server.tracing.storage.TraceSpan;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

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
    public void sendTrace(TraceMessage message) throws TException {
        log.info("Receiving trace message:{}", message);
        handler.submit(TraceSpan.from(message));
    }
}
