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

import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.observability.metric.model.IMeasurement;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.agent.observability.tracing.sampler.SamplingMode;
import org.bithon.agent.plugin.httpclient.javanethttp.JavaNetHttpClientPlugin;
import org.bithon.agent.plugins.test.AbstractPluginInterceptorTest;
import org.bithon.component.commons.tracing.Tags;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Test case for Java Net HTTP Client plugin (JDK 11+)
 * <p>
 * This test only runs on JDK 11 and above since java.net.http.HttpClient
 * was introduced in Java 11. The plugin itself uses JdkVersionPrecondition.gte(11)
 * to ensure it only loads on compatible JDK versions.
 * <p>
 * Maven profiles are configured to exclude this test on JDK versions below 11.
 *
 * @author frankchen
 */
public class JavaNetHttpClientPluginInterceptorTest extends AbstractPluginInterceptorTest {

    @Override
    protected IPlugin[] getPlugins() {
        return new IPlugin[]{new JavaNetHttpClientPlugin()};
    }

    /**
     * Override the parent test to ensure it runs first
     */
    @Test
    @Order(0)
    @Override
    public void testInterceptorInstallation() {
        super.testInterceptorInstallation();
    }

    @Test
    public void testSendMethod() throws Exception {
        ITraceContext ctx = TraceContextFactory.newContext(SamplingMode.FULL);
        ctx.currentSpan().name("ROOT");

        try (Closeable c = TraceContextHolder::detach) {
            TraceContextHolder.attach(ctx);

            Assertions.assertNotNull(TraceContextHolder.current());


            // Create HTTP request to a reliable endpoint
            HttpRequest request = HttpRequest.newBuilder()
                                             .uri(URI.create("https://github.com"))
                                             .GET()
                                             .build();

            // Send HTTP request using Java Net HTTP Client
            HttpResponse<String> response = HttpClient.newHttpClient()
                                                      .send(request, HttpResponse.BodyHandlers.ofString());

            // Verify the HTTP request was successful
            Assertions.assertTrue(response.statusCode() >= 200 && response.statusCode() < 400,
                                  "HTTP request should be successful, got status: " + response.statusCode());
            Assertions.assertNotNull(response.body(), "Response body should not be null");
        }
        ctx.currentSpan().finish();
        ctx.finish();

        // Verify tracing span logs
        Assertions.assertEquals(2, this.reportedSpans.size());
        Assertions.assertEquals("send", this.reportedSpans.get(0).method());
        Assertions.assertEquals("http-client", this.reportedSpans.get(0).name());
        Assertions.assertEquals("java.net.http", this.reportedSpans.get(0).tags().get(Tags.Http.CLIENT));
        Assertions.assertEquals("GET", this.reportedSpans.get(0).tags().get(Tags.Http.METHOD));
        Assertions.assertEquals("https://github.com", this.reportedSpans.get(0).tags().get(Tags.Http.URL));
        Assertions.assertEquals("200", this.reportedSpans.get(0).tags().get(Tags.Http.STATUS));

        // Wait for metrics to be exported
        Thread.sleep(11_000);

        // Verify metrics, See HttpOutgoingMetricsRegistry for definition
        Assertions.assertEquals(1, REPORTED_METRICS.size());
        IMeasurement measurement = REPORTED_METRICS.get(0);
        Assertions.assertEquals("https://github.com", measurement.getDimensions().getValue(0));
        Assertions.assertEquals("GET", measurement.getDimensions().getValue(1));
        Assertions.assertEquals("200", measurement.getDimensions().getValue(2));

        // See HttpOutgoingMetrics
        measurement.getMetricValue(0); // responseTime
        measurement.getMetricValue(1); // maxResponseTime
        measurement.getMetricValue(2); // minResponseTime
        Assertions.assertEquals(0, measurement.getMetricValue(3)); //count4xx
        Assertions.assertEquals(0, measurement.getMetricValue(4)); //count5xx
        Assertions.assertEquals(0, measurement.getMetricValue(5)); //countException
        Assertions.assertEquals(1, measurement.getMetricValue(6)); //requestCount
    }

    @Test
    public void testConnectionException() throws Exception {
        ITraceContext ctx = TraceContextFactory.newContext(SamplingMode.FULL);
        ctx.currentSpan().name("ROOT");

        try (Closeable c = TraceContextHolder::detach) {
            TraceContextHolder.attach(ctx);

            Assertions.assertNotNull(TraceContextHolder.current());


            // Create HTTP request to a reliable endpoint
            HttpRequest request = HttpRequest.newBuilder()
                                             .uri(URI.create("https://non-exists.com"))
                                             .GET()
                                             .build();

            // Send HTTP request using Java Net HTTP Client
            HttpResponse<String> response = HttpClient.newHttpClient()
                                                      .send(request, HttpResponse.BodyHandlers.ofString());

            // Verify the HTTP request was successful
            Assertions.assertTrue(response.statusCode() >= 200 && response.statusCode() < 400,
                                  "HTTP request should be successful, got status: " + response.statusCode());
            Assertions.assertNotNull(response.body(), "Response body should not be null");
        }
        ctx.currentSpan().finish();
        ctx.finish();

        // Verify tracing span logs
        Assertions.assertEquals(2, this.reportedSpans.size());
        Assertions.assertEquals("send", this.reportedSpans.get(0).method());
        Assertions.assertEquals("http-client", this.reportedSpans.get(0).name());
        Assertions.assertEquals("java.net.http", this.reportedSpans.get(0).tags().get(Tags.Http.CLIENT));
        Assertions.assertEquals("GET", this.reportedSpans.get(0).tags().get(Tags.Http.METHOD));
        Assertions.assertEquals("https://non-exists.com", this.reportedSpans.get(0).tags().get(Tags.Http.URL));
        Assertions.assertNotNull(this.reportedSpans.get(0).tags().get(Tags.Exception.TYPE));

        // Wait for metrics to be exported
        Thread.sleep(11_000);

        // Verify metrics, See HttpOutgoingMetricsRegistry for definition
        Assertions.assertEquals(1, REPORTED_METRICS.size());
        IMeasurement measurement = REPORTED_METRICS.get(0);
        Assertions.assertEquals("https://github.com", measurement.getDimensions().getValue(0));
        Assertions.assertEquals("GET", measurement.getDimensions().getValue(1));
        Assertions.assertEquals("", measurement.getDimensions().getValue(2));

        // See HttpOutgoingMetrics
        measurement.getMetricValue(0); // responseTime
        measurement.getMetricValue(1); // maxResponseTime
        measurement.getMetricValue(2); // minResponseTime
        Assertions.assertEquals(0, measurement.getMetricValue(3)); //count4xx
        Assertions.assertEquals(0, measurement.getMetricValue(4)); //count5xx
        Assertions.assertEquals(1, measurement.getMetricValue(5)); //countException
        Assertions.assertEquals(0, measurement.getMetricValue(6)); //requestCount
    }

    @Test
    public void testSendAsyncMethod() throws Exception {
        ITraceContext ctx = TraceContextFactory.newContext(SamplingMode.FULL);
        ctx.currentSpan().name("ROOT");

        try (Closeable c = TraceContextHolder::detach) {
            TraceContextHolder.attach(ctx);

            Assertions.assertNotNull(TraceContextHolder.current());


            // Create HTTP request to a reliable endpoint
            HttpRequest request = HttpRequest.newBuilder()
                                             .uri(URI.create("https://github.com"))
                                             .GET()
                                             .build();

            // Send HTTP request using Java Net HTTP Client
            HttpResponse<String> response = HttpClient.newHttpClient()
                                                      .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                                                      .get();

            // Verify the HTTP request was successful
            Assertions.assertTrue(response.statusCode() >= 200 && response.statusCode() < 400,
                                  "HTTP request should be successful, got status: " + response.statusCode());
            Assertions.assertNotNull(response.body(), "Response body should not be null");
        }
        ctx.currentSpan().finish();
        ctx.finish();

        Assertions.assertEquals(2, this.reportedSpans.size());
        Assertions.assertEquals("sendAsync", this.reportedSpans.get(0).method());
        Assertions.assertEquals("http-client", this.reportedSpans.get(0).name());
        Assertions.assertEquals("java.net.http", this.reportedSpans.get(0).tags().get(Tags.Http.CLIENT));
        Assertions.assertEquals("GET", this.reportedSpans.get(0).tags().get(Tags.Http.METHOD));
        Assertions.assertEquals("https://github.com", this.reportedSpans.get(0).tags().get(Tags.Http.URL));
        Assertions.assertEquals("200", this.reportedSpans.get(0).tags().get(Tags.Http.STATUS));
    }
}
