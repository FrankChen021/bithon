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

package org.bithon.agent.plugin.grpc.client.interceptor;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.context.PropagationTraceContext;
import org.bithon.agent.core.tracing.context.TraceSpanFactory;

/**
 * @author Frank Chen
 * @date 22/12/22 9:51 pm
 */
public class ClientCallInterceptor implements ClientInterceptor {
    private final String target;

    public ClientCallInterceptor(String target) {
        this.target = target;
    }

    @Override
    public <REQ, RSP> ClientCall<REQ, RSP> interceptCall(MethodDescriptor<REQ, RSP> method, CallOptions callOptions, Channel next) {
        // Must be the first, this would sometimes call DnsNameResolver
        ClientCall<REQ, RSP> result = next.newCall(method, callOptions);

        // Not sure when the other methods will be called,
        // so, it's better to create a new tracing context for this gRPC client call
        ITraceSpan span = TraceSpanFactory.newAsyncSpan("grpc-client")
                                          .clazz(ClientInterceptor.class.getName())
                                          .method("interceptCall")
                                          .tag("remote.address", this.target)
                                          .start();

        if (span.isNull() || span.context() instanceof PropagationTraceContext) {
            return result;
        }

        return new TracedClientCall<>(span, result);
    }

    static class TracedClientCall<REQ, RSP> extends ForwardingClientCall.SimpleForwardingClientCall<REQ, RSP> {
        private final ITraceSpan span;

        public TracedClientCall(ITraceSpan span, ClientCall<REQ, RSP> delegate) {
            super(delegate);
            this.span = span;
        }

        @Override
        public void start(Listener<RSP> responseListener, Metadata headers) {
            // Propagate the tracing context to remote server
            span.context().propagate(headers, (request, key, val) -> request.put(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER), val));

            try {
                // Hook the message listener to end the tracing span
                Listener<RSP> listener = new TracedClientCallListener<>(span, responseListener);

                super.start(listener, headers);
            } catch (Throwable t) {
                span.tag(t).finish();
                span.context().finish();
                throw t;
            }
        }

        @Override
        public void sendMessage(REQ message) {
            try {
                super.sendMessage(message);
            } catch (Throwable t) {
                span.tag(t).finish();
                span.context().finish();
                throw t;
            }
        }
    }

    static class TracedClientCallListener<RSP> extends ForwardingClientCallListener.SimpleForwardingClientCallListener<RSP> {
        private final ITraceSpan span;

        public TracedClientCallListener(ITraceSpan span, ClientCall.Listener<RSP> delegate) {
            super(delegate);
            this.span = span;
        }

        @Override
        public void onClose(Status status, Metadata trailers) {
            span.tag(status.getCause()).tag("status", status.getCode().toString()).finish();
            span.context().finish();

            super.onClose(status, trailers);
        }
    }
}
