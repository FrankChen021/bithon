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

package org.bithon.server.discovery.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.exception.ErrorResponse;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * @author Frank Chen
 * @date 21/4/23 2:02 pm
 */
public class ErrorResponseDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;

    public ErrorResponseDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(String methodKey, Response response) {

        try {
            String body = response.body() == null ? null : new String(Util.toByteArray(response.body().asInputStream()), StandardCharsets.UTF_8);

            String path = response.request().url();
            try {
                URL url = new URL(response.request().url());
                path = StringUtils.format("%s://%s:%d%s", url.getProtocol(), url.getHost(), url.getPort(), url.getPath());
            } catch (MalformedURLException ignored) {
            }

            // Try to decode the message
            Collection<String> contentTypeHeaders = response.headers().get("Content-Type");
            if (contentTypeHeaders != null
                && body != null
                && !contentTypeHeaders.isEmpty()
                && contentTypeHeaders.iterator().next().startsWith("application/json")) {
                ErrorResponse errorResponse = this.objectMapper.readValue(body, ErrorResponse.class);
                if (errorResponse.getException() != null) {
                    return new HttpMappableException(errorResponse.getException(),
                                                     response.status(),
                                                     errorResponse.getMessage());
                }
            }

            return new HttpMappableException(response.status(),
                                             "Error from remote [%s]: %s, Status = %d",
                                             path,
                                             body == null ? "" : body,
                                             response.status());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
