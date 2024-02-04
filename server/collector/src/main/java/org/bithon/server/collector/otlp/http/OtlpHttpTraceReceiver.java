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

package org.bithon.server.collector.otlp.http;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.collector.otlp.OtlpSpanConverter;
import org.bithon.server.pipeline.tracing.ITraceProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/9/1 20:59
 */
@Slf4j
@RestController
@ConditionalOnProperty(value = "bithon.receivers.traces.otlp-http.enabled", havingValue = "true")
public class OtlpHttpTraceReceiver {

    @Setter
    private ITraceProcessor processor;

    @PostMapping("/api/collector/otlp/trace")
    public void collect(HttpServletRequest request,
                        HttpServletResponse response) throws IOException {
        if (processor == null) {
            return;
        }

        InputStream is;
        String encoding = request.getHeader("Content-Encoding");
        if (!StringUtils.isEmpty(encoding)) {
            if ("gzip".equals(encoding)) {
                is = new GZIPInputStream(request.getInputStream());
            } else if ("deflate".equals(encoding)) {
                is = new InflaterInputStream(request.getInputStream());
            } else {
                String message = StringUtils.format("Not supported Content-Encoding [%s] from remote [%s]", encoding, request.getRemoteAddr());
                response.getWriter().println(message);
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                return;
            }
        } else {
            is = request.getInputStream();
        }

        OtlpSpanConverter spanConverter;
        if ("application/x-protobuf".equals(request.getContentType())) {
            spanConverter = OtlpSpanConverter.fromBinary(is);
        } else if ("application/json".equals(request.getContentType())) {
            spanConverter = OtlpSpanConverter.fromJson(is);
        } else {
            String message = StringUtils.format("Not supported Content-Type [%s] from remote [%s]", request.getContentType(), request.getRemoteAddr());
            response.getWriter().println(message);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return;
        }

        this.processor.process("trace",
                               spanConverter.toSpanList());
    }
}
