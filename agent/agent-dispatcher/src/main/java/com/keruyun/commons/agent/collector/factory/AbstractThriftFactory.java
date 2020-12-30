package com.keruyun.commons.agent.collector.factory;

import com.keruyun.commons.agent.collector.enums.TransportProtocolEnum;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.TTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Roy Wesley
 * @date 2018/1/23 13:20
 * @todo Thrift抽象工厂类
 */

public abstract class AbstractThriftFactory {

    protected static Logger logger = LoggerFactory.getLogger(AbstractThriftFactory.class);

    /**
     * 传输协议工厂类选择
     *
     * @param protocolEnum 传输协议枚举类
     * @return 返回传输协议工厂类
     */
    protected TProtocolFactory chooseProtocolFactory(TransportProtocolEnum protocolEnum) {
        TProtocolFactory protocolFactory;

        // 类型选择
        switch (protocolEnum) {
            case JSON:
                protocolFactory = new TJSONProtocol.Factory();
                break;
            case COMPACT:
                protocolFactory = new TCompactProtocol.Factory();
                break;
            case BINARY:
                protocolFactory = new TBinaryProtocol.Factory();
                break;
            default:
                protocolFactory = new TCompactProtocol.Factory();
        }

        return protocolFactory;
    }

    /**
     * 传输协议选择
     *
     * @param protocolEnum 传输协议枚举类
     * @return 返回传输协议工厂类
     */
    protected TProtocol chooseProtocol(TransportProtocolEnum protocolEnum, TTransport transport) {
        TProtocol protocol;

        // 类型选择
        switch (protocolEnum) {
            case JSON:
                protocol = new TJSONProtocol(transport);
                break;
            case COMPACT:
                protocol = new TCompactProtocol(transport);
                break;
            case BINARY:
                protocol = new TBinaryProtocol(transport);
                break;
            default:
                protocol = new TCompactProtocol(transport);
        }

        return protocol;
    }
}
