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

package org.bithon.server.sink.tracing.transform;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.sink.common.service.UriNormalizer;
import org.bithon.server.storage.common.ApplicationType;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.transformer.ITransformer;
import org.bithon.server.storage.tracing.TraceSpan;
import org.springframework.util.CollectionUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 30/3/22 10:50 PM
 */
public class TraceSpanTransformer implements ITransformer {

    private final UriNormalizer normalizer;

    @JsonCreator
    public TraceSpanTransformer(@JacksonInject(useInput = OptBoolean.FALSE) UriNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    /**
     * flatten some properties in tags.
     * SHOULD be called AFTER all properties have been set
     */
    @Override
    public void transform(IInputRow inputRow) throws TransformException {
        TraceSpan span = (TraceSpan) inputRow;

        transformIntoJavaStyleMethod(span);
        transformClickHouseSpans(span);

        Map<String, String> tags = span.getTags();
        if (CollectionUtils.isEmpty(tags)) {
            return;
        }

        //
        // Standardize tag names
        //
        String httpStatus = tags.remove("clickhouse.http_status");
        if (httpStatus != null) {
            tags.put(Tags.Http.STATUS, httpStatus);
        }

        String uri = tags.remove("clickhouse.uri");
        if (uri != null) {
            tags.put(Tags.Http.URL, uri);
        }

        String status = tags.getOrDefault("http.status", "");
        if ("".equals(status)) {
            // compatibility
            status = tags.getOrDefault("status", "");
        }
        if ("".equals(status)) {
            status = tags.containsKey("exception")
                    || tags.containsKey(Tags.Exception.MESSAGE)
                    || tags.containsKey(Tags.Exception.STACKTRACE)
                    || tags.containsKey(Tags.Exception.TYPE) ? "500" : "200";
        }
        span.setStatus(status);

        uri = tags.getOrDefault(Tags.Http.URL, "");
        if ("".equals(uri)) {
            // compatibility
            uri = tags.getOrDefault("uri", "");
        }

        // Normalize URL
        if (SpanKind.CLIENT.name().equals(span.getKind()) && "httpclient".equals(span.getName())) {
            // For the http.uri tag, if it's from the HttpClient, get the path only to normalize
            try {
                URL url = new URL(uri);
                String path = url.getPath();
                String query = url.getQuery();

                tags.remove(Tags.Http.URL);
                tags.put(Tags.Http.TARGET, query == null ? path : (path + "?" + query));
                tags.put(Tags.Net.PEER, url.getHost() + ":" + url.getPort());

                // Use the path to normalize
                uri = path;
            } catch (MalformedURLException ignored) {
            }
        }
        span.setNormalizedUri(normalizer.normalize(span.getAppName(), uri).getUri());
    }

    private void transformIntoJavaStyleMethod(TraceSpan span) {
        if (ApplicationType.JAVA.equals(span.appType)) {
            return;
        }

        // Discard the input parameters of the method
        int parameterStart = span.getMethod().indexOf('(');
        if (parameterStart > 0) {
            span.setMethod(span.getMethod().substring(0, parameterStart));
        }

        //
        // Change C++ style method name, which is separated by '::', into Java style
        //
        // First change to dot-separated calling style
        String fullQualifiedName = span.getMethod().replaceAll("::", ".");
        if (fullQualifiedName.endsWith("()")) {
            // Remove the ending parenthesis
            fullQualifiedName = fullQualifiedName.substring(0, fullQualifiedName.length() - 2);
        }
        int idx = fullQualifiedName.lastIndexOf(' ');
        if (idx >= 0) {
            // Discard the return-type
            fullQualifiedName = fullQualifiedName.substring(idx + 1);
        }

        idx = fullQualifiedName.lastIndexOf(".");
        if (idx >= 0) {
            // split the class the method
            span.setClazz(fullQualifiedName.substring(0, idx));
            span.setMethod(fullQualifiedName.substring(idx + 1));
        }
    }

    /**
     * For old ClickHouse spans, the 'kind' is missing, add kind to spans
     */
    private void transformClickHouseSpans(TraceSpan span) {
        if (StringUtils.isEmpty(span.getKind())) {
            if ("00".equals(span.getParentSpanId()) || "".equals(span.getParentSpanId())) {
                span.setParentSpanId("");
                span.setKind("SERVER");
            } else if ("HTTPHandler".equals(span.getClazz()) && "handleRequest".equals(span.getMethod())) {
                span.setKind("SERVER");
            } else if ("TCPHandler".equals(span.getMethod())) {
                span.setKind("SERVER");
            }
        }

        Map<String, String> tags = span.getTags();
        String httpStatus = tags.remove("clickhouse.http_status");
        if (httpStatus != null) {
            tags.put(Tags.Http.STATUS, httpStatus);
        }

        String uri = tags.remove("clickhouse.uri");
        if (uri != null) {
            tags.put(Tags.Http.URL, uri);
        }

        String threadId = tags.remove("clickhouse.thread_id");
        if (threadId != null) {
            tags.put(Tags.Thread.ID, threadId);
        }

        String exception = tags.remove("clickhouse.exception");
        if (exception != null) {
            tags.put(Tags.Exception.MESSAGE, exception);
        }

        String exceptionCode = tags.remove("clickhouse.exception_code");
        if (exceptionCode != null) {
            tags.put(Tags.Exception.CODE, exceptionCode);
        }
    }
}
