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
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.notification.channel.INotificationChannel;
import org.bithon.server.alerting.notification.config.NotificationProperties;
import org.bithon.server.alerting.notification.message.ExpressionEvaluationResult;
import org.bithon.server.alerting.notification.message.NotificationMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Notification via HTTP POST
 *
 * @author Frank Chen
 * @date 18/3/22 10:09 PM
 */
@Slf4j
public class HttpNotificationChannel implements INotificationChannel {

    @JsonIgnore
    protected final NotificationProperties notificationProperties;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HttpChannelProps {
        @NotBlank
        private String url;

        /**
         * Extra headers.
         * Can be NULL or empty
         */
        private Map<String, String> headers;

        /**
         * Explicitly defined header so that user must provide such configuration
         */
        @NotBlank
        private String contentType;

        /**
         * The body template. Supported variables:
         * {alert.appName}: The application name of the alert belonged to
         * {alert.name}: The name of the alert.
         * {alert.expr}: The expression that the alert runs on
         * {alert.url}: The url that users can view the detail of this alert record
         * {alert.message}: The default alert message
         */
        @NotBlank
        private String body;
    }

    @Getter
    protected final HttpChannelProps props;

    @JsonCreator
    public HttpNotificationChannel(@JsonProperty("props") HttpChannelProps props,
                                   @JacksonInject(useInput = OptBoolean.FALSE) NotificationProperties notificationProperties) {
        this.props = Preconditions.checkNotNull(props, "props property can not be null.");
        Preconditions.checkIfTrue(!StringUtils.isBlank(this.props.url), "The url property can not be empty");

        this.props.url = props.url.trim();
        this.props.headers = props.headers == null ? Collections.emptyMap() : props.headers;

        this.notificationProperties = notificationProperties;
    }

    @Override
    public void send(NotificationMessage message) throws IOException {
        String defaultMessage;
        if (message.getExpressions().size() == 1) {
            ExpressionEvaluationResult result = message.getConditionEvaluation().entrySet().iterator().next().getValue();
            defaultMessage = StringUtils.format("expected: %s, current: %s",
                                                result.getOutputs().getThreshold(),
                                                result.getOutputs().getCurrent());
        } else {
            defaultMessage = message.getConditionEvaluation()
                                    .entrySet()
                                    .stream()
                                    .map((entry) -> {
                                        AlertExpression evaluatedExpression = message.getExpressions()
                                                                                     .stream()
                                                                                     .filter((expr) -> expr.getId().equals(entry.getKey()))
                                                                                     .findFirst()
                                                                                     .orElse(null);

                                        ExpressionEvaluationResult result = entry.getValue();
                                        return StringUtils.format("expr: %s, expected: %s, current: %s",
                                                                  evaluatedExpression.serializeToText(),
                                                                  result.getOutputs().getThreshold(),
                                                                  result.getOutputs().getCurrent());
                                    })
                                    .collect(Collectors.joining("\n"));
        }
        String body = this.props.body.replace("{alert.appName}", StringUtils.getOrEmpty(message.getAlertRule().getAppName()))
                                     .replace("{alert.name}", message.getAlertRule().getName())
                                     .replace("{alert.expr}", message.getAlertRule().getExpr())
                                     .replace("{alert.url}", getURL(message))
                                     .replace("{alert.message}", defaultMessage);
        AbstractHttpEntity bodyEntity = serializeRequestBody(body);

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost request = new HttpPost(this.props.url);

            // Custom headers
            request.setHeaders(this.props
                                   .headers
                                   .keySet()
                                   .stream()
                                   .map(k -> new BasicHeader(k, this.props.headers.get(k)))
                                   .toArray(Header[]::new));

            // Mandatory header after custom header
            request.setHeader("Content-Type", props.contentType);

            // Body
            request.setEntity(bodyEntity);

            HttpResponse response = client.execute(request);
            if (response.getStatusLine().getStatusCode() >= 400) {
                throw new RuntimeException(StringUtils.format("Failed to send message to [%s]: Received status: %d, response: %s",
                                                              this.props.url,
                                                              response.getStatusLine().getStatusCode(),
                                                              IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8)));
            }
        }
    }

    @Override
    public String toString() {
        return "HttpNotificationChannel{" +
               "url='" + this.props.url + '\'' +
               '}';
    }

    protected AbstractHttpEntity serializeRequestBody(String body) throws IOException {
        return new StringEntity(body, StandardCharsets.UTF_8);
    }

    private String getURL(NotificationMessage message) {
        String url = notificationProperties.getManagerURL();
        if (StringUtils.isBlank(url)) {
            return "";
        }

        return url
               + (url.endsWith("/") ? "" : "/")
               + "web/alerting/record/detail?id="
               + message.getAlertRecordId();
    }
}
