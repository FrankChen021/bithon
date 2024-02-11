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

package org.bithon.server.alerting.notification.channel.http;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.bithon.server.alerting.notification.message.NotificationMessage;
import org.bithon.server.alerting.notification.channel.INotificationChannel;
import org.springframework.http.MediaType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * @author Frank Chen
 * @date 18/3/22 10:09 PM
 */
@Slf4j
public class HttpNotificationChannel implements INotificationChannel {

    @Getter
    private final String url;

    @Getter
    private final Map<String, String> headers;

    @JsonIgnore
    private final ObjectMapper objectMapper;

    @Getter
    @Setter
    @JsonIgnore
    private String name;

    @JsonCreator
    public HttpNotificationChannel(@JsonProperty("url") @Nonnull String url,
                                   @JsonProperty("headers") @Nullable Map<String, String> headers,
                                   @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper) {
        this.url = url;
        this.headers = headers == null ? Collections.emptyMap() : headers;
        this.objectMapper = objectMapper;
    }

    @Override
    public void notify(NotificationMessage message) {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        try (CloseableHttpClient client = httpClientBuilder.build()) {
            HttpPost request = new HttpPost(this.url);
            request.setEntity(new StringEntity(objectMapper.writeValueAsString(message), MediaType.APPLICATION_JSON_VALUE));

            request.setHeaders(headers.keySet().stream().map(k -> new BasicHeader(k, headers.get(k))).toArray(Header[]::new));
            HttpResponse response = client.execute(request);
            if (response.getStatusLine().getStatusCode() != 200) {
                log.error("Response: code={}, content={}",
                          response.getStatusLine(),
                          IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            log.error("Exception when send alert via http", e);
        }
    }

    @Override
    public String toString() {
        return "HttpNotificationProvider{" +
               "url='" + url + '\'' +
               '}';
    }
}
