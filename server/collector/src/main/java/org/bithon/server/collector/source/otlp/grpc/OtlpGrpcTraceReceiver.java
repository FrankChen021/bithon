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

package org.bithon.server.collector.source.otlp.grpc;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.trace.v1.ExportTracePartialSuccess;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.collector.source.otlp.OtlpSpanConverter;
import org.bithon.server.pipeline.tracing.ITraceProcessor;
import org.bithon.server.pipeline.tracing.receiver.ITraceReceiver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;

import java.io.IOException;

/**
 * https://opentelemetry.io/docs/specs/otlp/#otlpgrpc
 *
 * @author Frank Chen
 * @date 30/1/24 8:35 pm
 */
@Slf4j
@JsonTypeName("otlp-trace-grpc")
@ConditionalOnProperty(value = "bithon.receivers.traces.otlp-grpc.enabled", havingValue = "true")
public class OtlpGrpcTraceReceiver extends TraceServiceGrpc.TraceServiceImplBase implements ITraceReceiver {

    private Server server;
    private final int port;
    private ITraceProcessor traceProcessor;

    @JsonCreator
    public OtlpGrpcTraceReceiver(@JacksonInject(useInput = OptBoolean.FALSE) Environment env) {
        // https://opentelemetry.io/docs/specs/otlp/#otlpgrpc-default-port
        port = env.getProperty("bithon.receivers.traces.otel-grpc.port", int.class, 4317);
    }

    @Override
    public void registerProcessor(ITraceProcessor processor) {
        this.traceProcessor = processor;
    }

    @Override
    public void start() {
        log.info("Starting GRPC trace receiver at port {}", this.port);
        server = ServerBuilder.forPort(this.port)
                              .addService(this)
                              .build();
        try {
            server.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        log.info("Shutdown GRPC receiver at port {}", this.port);
        try {
            server.shutdownNow();
            server.awaitTermination();
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public void export(ExportTraceServiceRequest request, StreamObserver<ExportTraceServiceResponse> responseObserver) {
        if (traceProcessor != null) {
            traceProcessor.process("trace",
                                   new OtlpSpanConverter(request.getResourceSpansList()).toSpanList());
        }

        ExportTraceServiceResponse response = ExportTraceServiceResponse.newBuilder()
                                                                        .setPartialSuccess(ExportTracePartialSuccess.newBuilder()
                                                                                                                    .setRejectedSpans(0)
                                                                                                                    .build())
                                                                        .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
