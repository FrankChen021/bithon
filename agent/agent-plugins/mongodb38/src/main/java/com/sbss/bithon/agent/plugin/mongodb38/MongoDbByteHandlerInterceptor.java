package com.sbss.bithon.agent.plugin.mongodb38;

import com.mongodb.connection.ConnectionId;
import com.mongodb.internal.connection.InternalStreamConnection;
import com.mongodb.internal.connection.ResponseBuffers;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import org.bson.ByteBuf;

import java.util.List;

/**
 * @author frankchen
 */
public class MongoDbByteHandlerInterceptor extends AbstractInterceptor {
    private MongoMetricProvider counter;

    @Override
    public boolean initialize() {
        counter = MongoMetricProvider.getInstance();
        return true;
    }

    @Override
    public void onMethodLeave(AopContext context) {
        if (context.getMethod().getName().contains("sendMessage")) {
            recordBytesOut(context);
        } else {
            recordBytesIn(context);
        }
    }

    private void recordBytesIn(AopContext aopContext) {
        ResponseBuffers result = (ResponseBuffers) aopContext.castReturningAs();
        int bytesIn = result.getReplyHeader().getMessageLength();

        InternalStreamConnection target = (InternalStreamConnection) aopContext.getTarget();
        ConnectionId connectionId = target.getDescription().getConnectionId();

        this.counter.recordBytesIn(connectionId.toString(), bytesIn);
    }

    private void recordBytesOut(AopContext aopContext) {
        InternalStreamConnection target = (InternalStreamConnection) aopContext.getTarget();

        List<ByteBuf> byteBufList = (List<ByteBuf>) aopContext.getArgs()[0];
        ConnectionId connectionId = target.getDescription().getConnectionId();
        int bytesOut = getMessageSize(byteBufList);

        this.counter.recordBytesOut(connectionId.toString(), bytesOut);
    }

    private int getMessageSize(final List<ByteBuf> byteBuffers) {
        int messageSize = 0;
        for (ByteBuf buffer : byteBuffers) {
            messageSize += buffer.remaining();
        }
        return messageSize;
    }
}
