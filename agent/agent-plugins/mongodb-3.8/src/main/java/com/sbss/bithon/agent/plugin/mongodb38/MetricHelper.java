package com.sbss.bithon.agent.plugin.mongodb38;

import org.bson.ByteBuf;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/27 17:36
 */
public class MetricHelper {

    public static int getMessageSize(final List<ByteBuf> byteBuffers) {
        int messageSize = 0;
        for (ByteBuf buffer : byteBuffers) {
            messageSize += buffer.remaining();
        }
        return messageSize;
    }
}
