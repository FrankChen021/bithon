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

import feign.FeignException;
import feign.Response;
import feign.Util;
import feign.codec.Decoder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Support to decode a Feign response into a {@link ResponseEntity} containing a {@link StreamingResponseBody}.
 *
 * @author frank.chen021@outlook.com
 * @date 2025/3/29 18:27
 */
public class StreamingResponseBodyDecoder implements Decoder {

    private final Decoder delegate;

    public StreamingResponseBodyDecoder(Decoder delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object decode(Response response, Type type) throws IOException, FeignException {
        if (type instanceof ParameterizedType parameterizedType) {
            if (ResponseEntity.class.equals(parameterizedType.getRawType())) {
                Type responseEntityType = parameterizedType.getActualTypeArguments()[0];
                if (StreamingResponseBody.class.equals(responseEntityType)) {
                    return handleStreamingResponseBody(response);
                }
            }
        }

        // For other types, use the delegate decoder and ensure the response is closed properly
        try {
            return delegate.decode(response, type);
        } finally {
            Util.ensureClosed(response);
        }
    }

    private ResponseEntity<StreamingResponseBody> handleStreamingResponseBody(Response response) {
        HttpHeaders headers = new HttpHeaders();
        for (Map.Entry<String, Collection<String>> entry : response.headers().entrySet()) {
            String headerName = entry.getKey();
            // Filter hop-by-hop headers and others not suitable for direct proxying
            if (entry.getValue() != null && !headerName.equalsIgnoreCase("Transfer-Encoding")
                && !headerName.equalsIgnoreCase("Connection")
                && !headerName.equalsIgnoreCase("Keep-Alive")
                && !headerName.equalsIgnoreCase("Public-Key-Pins")
                && !headerName.equalsIgnoreCase("Strict-Transport-Security")
                && !headerName.equalsIgnoreCase("Server")) {
                headers.addAll(headerName, new ArrayList<>(entry.getValue()));
            }
        }

        HttpStatus status = HttpStatus.valueOf(response.status());
        Response.Body body = response.body();

        if (body == null) {
            // Close the original response now if there's nobody to stream
            response.close();

            // Should not happen for a successful stream with data, but handle defensively
            return new ResponseEntity<>(outputStream -> {
            }, headers, status);
        }

        // The StreamingResponseBody takes responsibility for the feign.Response.Body's InputStream
        StreamingResponseBody streamingBody = outputStream -> {
            try (InputStream inputStreamFromServer = body.asInputStream()) { // This stream must be closed
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStreamFromServer.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                }
            } catch (IOException e) {
                // This exception indicates a problem during streaming to the client.
                // The client connection might be broken.
                throw new IOException("Error streaming from Feign response to client: " + e.getMessage(), e);
            } finally {
                response.close();
            }
        };

        // Do NOT close 'response' here; its InputStream is now managed by the streamingBody.
        // It will be closed within the streamingBody lambda's finally block.
        return new ResponseEntity<>(streamingBody, headers, status);
    }
}
