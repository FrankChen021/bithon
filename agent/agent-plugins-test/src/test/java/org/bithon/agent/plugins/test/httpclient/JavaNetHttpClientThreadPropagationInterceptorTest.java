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

package org.bithon.agent.plugins.test.httpclient;

import com.sun.net.httpserver.HttpServer;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.java.adaptor.Java9Adaptor;
import org.bithon.agent.java.adaptor.JavaAdaptorFactory;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.agent.observability.tracing.context.propagation.w3c.W3CTraceContextHeader;
import org.bithon.agent.observability.tracing.sampler.SamplingMode;
import org.bithon.agent.plugin.httpclient.javanethttp.JavaNetHttpClientPlugin;
import org.bithon.agent.plugins.test.AbstractPluginInterceptorTest;
import org.bithon.component.commons.tracing.Tags;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledForJreRange(min = JRE.JAVA_11)
public class JavaNetHttpClientThreadPropagationInterceptorTest extends AbstractPluginInterceptorTest {

    private static final Object INTERCEPTOR_INSTALL_LOCK = new Object();
    private static boolean interceptorInstalled;

    @Override
    protected IPlugin[] getPlugins() {
        return new IPlugin[]{new JavaNetHttpClientPlugin()};
    }

    @Test
    @Order(0)
    @Override
    public void testInterceptorInstallation() {
        installInterceptor();
    }

    private void installInterceptor() {
        synchronized (INTERCEPTOR_INSTALL_LOCK) {
            if (interceptorInstalled) {
                return;
            }

            JavaAdaptorFactory.setAdaptor(new Java9Adaptor(ByteBuddyAgent.getInstrumentation()));
            super.testInterceptorInstallation();
            interceptorInstalled = true;
        }
    }

    @Test
    public void testSendAsyncPropagatesWorkerAsyncTaskSpanContext() throws Exception {
        installInterceptor();

        AtomicReference<String> traceParent = new AtomicReference<>();
        HttpServer server = newTraceServer(traceParent);

        ITraceContext ctx = TraceContextFactory.newContext(SamplingMode.FULL);
        ctx.currentSpan().name("ROOT");
        String rootSpanId = ctx.currentSpan().spanId();

        try (TracingExecutor executor = new TracingExecutor()) {
            try (Closeable ignored = TraceContextHolder::detach) {
                TraceContextHolder.attach(ctx);

                HttpRequest request = HttpRequest.newBuilder()
                                                 .uri(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/trace"))
                                                 .GET()
                                                 .build();

                CompletableFuture<HttpResponse<String>> responseFuture = HttpClient.newBuilder()
                                                                                   .executor(executor)
                                                                                   .build()
                                                                                   .sendAsync(request, HttpResponse.BodyHandlers.ofString());

                Assertions.assertEquals(rootSpanId, TraceContextHolder.current().currentSpan().spanId());

                HttpResponse<String> response = responseFuture.get();

                Assertions.assertEquals(200, response.statusCode());
            }
            ctx.currentSpan().finish();
            ctx.finish();
        } finally {
            server.stop(0);
        }

        Assertions.assertNotNull(traceParent.get());
        String propagatedParentSpanId = parentSpanId(traceParent.get());
        Assertions.assertNotEquals(rootSpanId, propagatedParentSpanId);

        ITraceSpan asyncTaskSpan = awaitSpan(span -> Objects.equals(propagatedParentSpanId, span.spanId()));
        ITraceSpan httpClientSpan = awaitSpan(span -> "http-client".equals(span.name()) && "sendAsync".equals(span.method()));
        ITraceSpan threadPoolSpan = awaitSpan(span -> Objects.equals(asyncTaskSpan.parentSpanId(), span.spanId()));

        Assertions.assertEquals("async-task", asyncTaskSpan.name());
        Assertions.assertEquals("thread-pool", threadPoolSpan.name());
        Assertions.assertEquals("execute", threadPoolSpan.method());
        Assertions.assertEquals(rootSpanId, httpClientSpan.parentSpanId());
        Assertions.assertEquals(httpClientSpan.spanId(), threadPoolSpan.parentSpanId());
        Assertions.assertEquals("HttpClient-test-Worker", asyncTaskSpan.tags().get(Tags.Thread.NAME));
        Assertions.assertNotEquals(httpClientSpan.spanId(), propagatedParentSpanId);
    }

    private static HttpServer newTraceServer(AtomicReference<String> traceParent) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/trace", exchange -> {
            traceParent.set(exchange.getRequestHeaders().getFirst(W3CTraceContextHeader.TRACE_HEADER_PARENT));
            byte[] response = "OK".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        return server;
    }

    private static ITraceSpan awaitSpan(Predicate<ITraceSpan> predicate) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            synchronized (REPORTED_SPANS) {
                for (ITraceSpan span : REPORTED_SPANS) {
                    if (predicate.test(span)) {
                        return span;
                    }
                }
            }
            Thread.sleep(10);
        }

        Assertions.fail("Expected span not reported: " + REPORTED_SPANS);
        return null;
    }

    private static String parentSpanId(String traceParent) {
        return traceParent.split("-")[2];
    }

    private static class TracingExecutor implements Executor, Closeable {
        private final ExecutorService delegate = Executors.newFixedThreadPool(1, runnable -> new Thread(runnable, "HttpClient-test-Worker"));

        @Override
        public void execute(Runnable command) {
            ITraceSpan threadPoolSpan = TraceContextFactory.newSpan("thread-pool");
            if (threadPoolSpan != null) {
                threadPoolSpan.method(ExecutorService.class.getName(), "execute")
                              .start();
            }

            ITraceSpan taskSpan;
            try {
                taskSpan = TraceContextFactory.newAsyncSpan("async-task");
                if (taskSpan != null) {
                    taskSpan.method(command.getClass().getName(), "run");
                }
            } finally {
                if (threadPoolSpan != null) {
                    threadPoolSpan.finish();
                }
            }

            delegate.execute(() -> {
                if (taskSpan == null) {
                    command.run();
                    return;
                }

                Throwable exception = null;
                TraceContextHolder.attach(taskSpan.context());
                taskSpan.start();
                try {
                    command.run();
                } catch (Throwable e) {
                    exception = e;
                    throw e;
                } finally {
                    Thread currentThread = Thread.currentThread();
                    taskSpan.tag(Tags.Thread.NAME, currentThread.getName())
                            .tag(Tags.Thread.ID, currentThread.getId())
                            .tag(exception)
                            .finish();
                    taskSpan.context().finish();
                    TraceContextHolder.detach();
                }
            });
        }

        @Override
        public void close() {
            delegate.shutdownNow();
        }
    }
}
