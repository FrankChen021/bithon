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
import io.jaegertracing.thriftjava.Batch;
import io.jaegertracing.thriftjava.Span;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.protocol.TCompactProtocol;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.server.pipeline.tracing.ITraceProcessor;
import org.bithon.server.pipeline.tracing.receiver.ITraceReceiver;
import org.bithon.server.storage.tracing.TraceSpan;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@JsonTypeName("jaeger-trace-udp")
public class JaegerThriftUDPTraceReceiver implements ITraceReceiver {

    private static final int MAX_PACKET_SIZE = 65000;
    private static final int DEFAULT_PORT = 6831;

    private final int port;
    private ITraceProcessor processor;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private DatagramSocket socket;
    private ExecutorService executorService;

    @JsonCreator
    public JaegerThriftUDPTraceReceiver(@JacksonInject(useInput = OptBoolean.FALSE) Environment env) {
        this.port = env.getProperty("bithon.receivers.traces.jaeger.port", int.class, DEFAULT_PORT);
    }

    @Override
    public void registerProcessor(ITraceProcessor processor) {
        this.processor = processor;
    }

    @Override
    public void start() {
        log.info("Starting jaeger-trace-udp receiver at port {}", this.port);
        try {
            socket = new DatagramSocket(port);
            executorService = Executors.newSingleThreadExecutor(NamedThreadFactory.daemonThreadFactory("jaeger-udp-receiver"));
            executorService.submit(this::receiveLoop);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        log.info("Stopping jaeger-trace-udp receiver at port {}", this.port);

        running.set(false);
        if (socket != null) {
            socket.close();
        }
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @SneakyThrows
    private void receiveLoop() {
        byte[] buffer = new byte[MAX_PACKET_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        TDeserializer deserializer = new TDeserializer(new TCompactProtocol.Factory());

        while (running.get()) {
            try {
                socket.receive(packet);

                // Copy the data since packet.getData() returns the shared buffer
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());

                processPacket(deserializer, data);
            } catch (IOException e) {
                if (running.get()) {
                    log.error("Error receiving UDP packet", e);
                }
            } catch (Exception e) {
                log.error("Error processing UDP packet", e);
            }
        }
    }

    private void processPacket(TDeserializer deserializer, byte[] data) throws Exception {
        if (processor == null) {
            return;
        }

        Batch batch = new Batch();
        deserializer.deserialize(batch, data);

        List<TraceSpan> spans = new ArrayList<>();
        for (Span jaegerSpan : batch.getSpans()) {
            TraceSpan span = JaegerSpanConverter.convert(jaegerSpan);
            span.appName = batch.process.serviceName;

            spans.add(span);
        }

        processor.process("trace", spans);
    }
}
