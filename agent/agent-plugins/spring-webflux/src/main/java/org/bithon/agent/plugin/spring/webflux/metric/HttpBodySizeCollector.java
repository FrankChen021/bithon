/*
 *    Copyright 2020 bithon.org
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

package org.bithon.agent.plugin.spring.webflux.metric;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import org.bithon.agent.bootstrap.aop.IBithonObject;
import reactor.netty.channel.ChannelOperations;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

/**
 * @author Frank ChenØ
 * @date 7/10/21 4:20 pm
 */
public abstract class HttpBodySizeCollector extends ChannelDuplexHandler {

    protected static final Logger LOG = LoggerFactory.getLogger(HttpBodySizeCollector.class);

    long dataReceived;
    long dataSent;

    @Override
    @SuppressWarnings("FutureReturnValueIgnored")
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (msg instanceof HttpResponse) {
            if (((HttpResponse) msg).status().equals(HttpResponseStatus.CONTINUE)) {
                //"FutureReturnValueIgnored" this is deliberate
                ctx.write(msg, promise);
                return;
            }
        }

        if (msg instanceof ByteBufHolder) {
            dataSent += ((ByteBufHolder) msg).content().readableBytes();
        } else if (msg instanceof ByteBuf) {
            dataSent += ((ByteBuf) msg).readableBytes();
        }

        if (msg instanceof LastHttpContent) {
            promise.addListener(future -> {
                ChannelOperations<?, ?> channelOps = ChannelOperations.get(ctx.channel());
                if (getTargetClass() != null && channelOps.getClass().isAssignableFrom(getTargetClass())) {
                    recordWrite(channelOps, dataSent);
                }

                dataSent = 0;
            });
        }

        //"FutureReturnValueIgnored" this is deliberate
        ctx.write(msg, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof ByteBufHolder) {
            dataReceived += ((ByteBufHolder) msg).content().readableBytes();
        } else if (msg instanceof ByteBuf) {
            dataReceived += ((ByteBuf) msg).readableBytes();
        }

        if (msg instanceof LastHttpContent) {
            ChannelOperations<?, ?> channelOps = ChannelOperations.get(ctx.channel());
            if (getTargetClass() != null && channelOps.getClass().isAssignableFrom(getTargetClass())) {
                recordRead(channelOps, dataReceived);
            }

            dataReceived = 0;
        }

        ctx.fireChannelRead(msg);
    }

    // TODO: use right statusCode
    private void recordRead(ChannelOperations<?, ?> channelOps, long dataReceived) {
        HttpIOMetrics metric = getMetricContext((IBithonObject) channelOps);
        if (metric.responseBytes == -1) {
            metric.requestBytes = dataReceived;
            return;
        }

        updateBytes(channelOps, dataReceived, metric.responseBytes);
    }

    private void recordWrite(ChannelOperations<?, ?> channelOps, long dataSent) {
        HttpIOMetrics metric = getMetricContext((IBithonObject) channelOps);
        if (metric.requestBytes == -1) {
            metric.responseBytes = dataSent;
            return;
        }

        updateBytes(channelOps, metric.requestBytes, dataSent);
    }

    protected abstract void updateBytes(ChannelOperations<?, ?> channelOps, long dataReceived, long dataSent);

    protected abstract HttpIOMetrics getMetricContext(IBithonObject bithonObject);

    protected abstract Class<?> getTargetClass();
}
