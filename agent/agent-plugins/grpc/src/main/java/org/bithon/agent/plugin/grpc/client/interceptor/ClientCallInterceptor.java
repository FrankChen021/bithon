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
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import org.bithon.agent.plugin.grpc.client.context.ObservabilityContext;
import org.bithon.agent.plugin.grpc.utils.MessageUtils;

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

        return new TracedClientCall<>(new ObservabilityContext(this.target, method.getFullMethodName()),
                                      result);
    }

    static class TracedClientCall<REQ, RSP> extends ForwardingClientCall.SimpleForwardingClientCall<REQ, RSP> {
        private final ObservabilityContext context;

        public TracedClientCall(ObservabilityContext context, ClientCall<REQ, RSP> delegate) {
            super(delegate);
            this.context = context;
        }

        @Override
        public void start(Listener<RSP> responseListener, Metadata headers) {
            // Propagate the tracing context to remote server
            if (context.getSpan() != null) {
                context.getSpan()
                       .context()
                       .propagate(headers,
                                  (request, key, val) -> request.put(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER), val));

            }
            try {
                // Hook the message listener to end the tracing span
                super.start(new TracedClientCallListener<>(context, responseListener),
                            headers);
            } catch (Throwable t) {
                context.finish(t);
                throw t;
            }
        }

        @Override
        public void sendMessage(REQ message) {
            int size = MessageUtils.getMessageSize(message);
            if (size > 0) {
                context.setRequestSize(size);
            }
            try {
                super.sendMessage(message);
            } catch (Throwable t) {
                context.finish(t);
                throw t;
            }
        }
    }

    static class TracedClientCallListener<RSP> extends ClientCall.Listener<RSP> {
        private final ClientCall.Listener<RSP> delegate;
        private final ObservabilityContext context;

        public TracedClientCallListener(ObservabilityContext context, ClientCall.Listener<RSP> delegate) {
            this.delegate = delegate;
            this.context = context;
        }

        @Override
        public void onHeaders(Metadata headers) {
            delegate.onHeaders(headers);
        }

        @Override
        public void onMessage(RSP message) {
            int size = MessageUtils.getMessageSize(message);
            if (size > 0) {
                context.setResponseSize(size);
            }
            delegate.onMessage(message);
        }

        @Override
        public void onReady() {
            delegate.onReady();
        }

        @Override
        public void onClose(Status status, Metadata trailers) {
            // TODO: Unify the status code
            // Currently we use 200 to represent OK so that the code comply with HTTP Status
            context.finish(status.equals(Status.OK) ? "200" : status.getCode().toString(),
                           status.getCause());

            delegate.onClose(status, trailers);
        }
    }
}
