package com.sbss.bithon.agent.rpc.thrift.endpoint;

import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.TTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author frankchen
 */
public abstract class AbstractThriftFactory {

    protected static Logger logger = LoggerFactory.getLogger(AbstractThriftFactory.class);

    protected TProtocolFactory createProtocolFactory(TransportProtocolEnum protocolEnum) {
        switch (protocolEnum) {
            case JSON:
                return new TJSONProtocol.Factory();
            case BINARY:
                return new TBinaryProtocol.Factory();
            case COMPACT:
            default:
                return new TCompactProtocol.Factory();
        }
    }

    protected TProtocol createProtocol(TransportProtocolEnum protocolEnum, TTransport transport) {
        switch (protocolEnum) {
            case JSON:
                return new TJSONProtocol(transport);
            case BINARY:
                return new TBinaryProtocol(transport);
            case COMPACT:
            default:
                return new TCompactProtocol(transport);
        }
    }
}
