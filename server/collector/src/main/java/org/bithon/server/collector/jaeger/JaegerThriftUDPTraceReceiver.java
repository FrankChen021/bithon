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

package org.bithon.server.collector.jaeger;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.google.common.annotations.VisibleForTesting;
import io.jaegertracing.thriftjava.Batch;
import io.jaegertracing.thriftjava.Span;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.transport.TMemoryInputTransport;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.server.pipeline.tracing.ITraceProcessor;
import org.bithon.server.pipeline.tracing.receiver.ITraceReceiver;
import org.bithon.server.storage.tracing.TraceSpan;
import org.bithon.shaded.io.netty.bootstrap.Bootstrap;
import org.bithon.shaded.io.netty.buffer.ByteBuf;
import org.bithon.shaded.io.netty.channel.Channel;
import org.bithon.shaded.io.netty.channel.ChannelHandlerContext;
import org.bithon.shaded.io.netty.channel.ChannelInitializer;
import org.bithon.shaded.io.netty.channel.ChannelOption;
import org.bithon.shaded.io.netty.channel.EventLoopGroup;
import org.bithon.shaded.io.netty.channel.SimpleChannelInboundHandler;
import org.bithon.shaded.io.netty.channel.nio.NioEventLoopGroup;
import org.bithon.shaded.io.netty.channel.socket.DatagramPacket;
import org.bithon.shaded.io.netty.channel.socket.nio.NioDatagramChannel;
import org.springframework.core.env.Environment;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A UDP receiver for Jaeger Thrift protocol using Netty.
 * It listens for incoming UDP packets, deserializes them into Jaeger spans,
 * and processes them using the provided trace processor.
 *
 * @author frank.chen021@outlook.com
 */
@Slf4j
@JsonTypeName("jaeger-trace-udp")
public class JaegerThriftUDPTraceReceiver implements ITraceReceiver {

    private static final int DEFAULT_PORT = 6831;

    /**
     * Default thread count for UDP receiver.
     * For I/O-bound operations like UDP packet processing, we can use more threads
     * than CPU cores since threads spend most time waiting for network I/O.
     * <p>
     * Strategy:
     * - Small systems (≤4 cores): Use all cores (good for development/testing)
     * - Medium systems (5-8 cores): Use cores - 1 (leave one for system)
     * - Large systems (>8 cores): Use cores - 2 (leave some for system overhead)
     */
    private static final int DEFAULT_THREADS = calculateDefaultThreads();

    private static int calculateDefaultThreads() {
        int cores = Runtime.getRuntime().availableProcessors();
        if (cores <= 4) {
            return cores; // Use all cores for small systems
        } else if (cores <= 8) {
            return cores - 1; // Leave 1 core for system
        } else {
            return cores - 2; // Leave 2 cores for system overhead
        }
    }

    private final int port;
    private final int threads;
    private ITraceProcessor processor;
    private EventLoopGroup group;
    private Channel channel;

    @JsonCreator
    public JaegerThriftUDPTraceReceiver(@JacksonInject(useInput = OptBoolean.FALSE) Environment env) {
        this.port = env.getProperty("bithon.receivers.traces.jaeger-udp.port", int.class, DEFAULT_PORT);
        this.threads = env.getProperty("bithon.receivers.traces.jaeger-udp.threads", int.class, DEFAULT_THREADS);
    }

    @VisibleForTesting
    public JaegerThriftUDPTraceReceiver(int port) {
        this.port = port;
        this.threads = DEFAULT_THREADS;
    }

    @VisibleForTesting
    public JaegerThriftUDPTraceReceiver(int port, int threads) {
        this.port = port;
        this.threads = Math.max(1, threads); // Ensure at least 1 thread
    }

    @Override
    public void registerProcessor(ITraceProcessor processor) {
        this.processor = processor;
    }

    @Override
    public void start() {
        log.info("Starting jaeger-trace-udp receiver at port {} with {} threads", this.port, this.threads);
        try {
            group = new NioEventLoopGroup(threads, NamedThreadFactory.daemonThreadFactory("jaeger-udp-receiver"));

            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                     .channel(NioDatagramChannel.class)
                     .option(ChannelOption.SO_BROADCAST, true)
                     .option(ChannelOption.SO_RCVBUF, 65536)
                     .handler(new ChannelInitializer<NioDatagramChannel>() {
                         @Override
                         protected void initChannel(NioDatagramChannel ch) {
                             ch.pipeline().addLast(new JaegerUDPHandler());
                         }
                     });

            channel = bootstrap.bind(new InetSocketAddress(port)).sync().channel();
        } catch (Exception e) {
            log.error("Failed to start jaeger-trace-udp receiver", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        log.info("Stopping jaeger-trace-udp receiver at port {}", this.port);

        try {
            if (channel != null) {
                channel.close().sync();
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while closing channel", e);
            Thread.currentThread().interrupt();
        } finally {
            if (group != null) {
                try {
                    group.shutdownGracefully(0, 5, TimeUnit.SECONDS).sync();
                } catch (InterruptedException e) {
                    log.warn("Interrupted while shutting down event loop group", e);
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private class JaegerUDPHandler extends SimpleChannelInboundHandler<DatagramPacket> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
            try {
                ByteBuf content = packet.content();

                if (content.hasArray()) {
                    byte[] data = content.array();
                    int offset = content.arrayOffset() + content.readerIndex();
                    int length = content.readableBytes();
                    processPacket(data, offset, length);
                } else {
                    // If no backing array, we need to copy (but this is rare for UDP packets)
                    int length = content.readableBytes();
                    byte[] data = new byte[length];
                    content.getBytes(content.readerIndex(), data, 0, length);
                    processPacket(data, 0, length);
                }
            } catch (Exception e) {
                log.error("Error processing received packet", e);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("Error in UDP handler", cause);
        }

        private void processPacket(byte[] data, int offset, int length) {
            if (processor == null) {
                return;
            }

            try {
                Batch batch = new Batch();
                batch.read(new TCompactProtocol(new TMemoryInputTransport(data, offset, length)));

                ApplicationInstance instance = ApplicationInstance.from(batch);
                List<TraceSpan> spans = new ArrayList<>();
                for (Span jaegerSpan : batch.getSpans()) {
                    TraceSpan span = JaegerSpanConverter.convert(instance, jaegerSpan);
                    span.appName = batch.getProcess().getServiceName();
                    spans.add(span);
                }

                processor.process("trace", spans);
            } catch (TException e) {
                log.error("Failed to deserialize Jaeger batch", e);
            }
        }
    }
}
