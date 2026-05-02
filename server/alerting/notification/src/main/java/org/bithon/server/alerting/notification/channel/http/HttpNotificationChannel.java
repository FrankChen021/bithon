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
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.DnsResolver;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.notification.channel.INotificationChannel;
import org.bithon.server.alerting.notification.config.NotificationProperties;
import org.bithon.server.alerting.notification.message.NotificationMessage;
import org.bithon.server.storage.alerting.pojo.AlertStatus;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
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
        validateTargetUrl(this.props.url);
        this.props.headers = props.headers == null ? Collections.emptyMap() : props.headers;

        this.notificationProperties = notificationProperties;
    }

    @Override
    public void send(NotificationMessage message) throws IOException {
        send(message, Duration.ofSeconds(30));
    }

    @Override
    public void test(NotificationMessage message, Duration timeout) throws Exception {
        send(message, timeout);
    }

    private void send(NotificationMessage message, Duration timeout) throws IOException {
        String messageBody = StringUtils.hasText(message.getAlertRule().getNotificationProps().getMessage()) ?
                             message.getAlertRule().getNotificationProps().getMessage() : this.props.body;

        messageBody = messageBody.replace("{alert.appName}", StringUtils.getOrEmpty(message.getAlertRule().getAppName()))
                                 .replace("{alert.name}", message.getAlertRule().getName())
                                 .replace("{alert.expr}", message.getAlertRule().getExpr())
                                 .replace("{alert.url}", getURL(message))
                                 .replace("{alert.status}", message.getStatus().name());

        String evaluationMessage = "";
        if (message.getStatus() == AlertStatus.ALERTING) {
            evaluationMessage = "\n" +
                                message.getEvaluationOutputs()
                                       .values()
                                       .stream()
                                       .flatMap(Collection::stream)
                                       .map((output) -> StringUtils.format("%s = %s, expected: %s, delta: %s",
                                                                           output.getLabel().isEmpty() ? "current" : output.getLabel().formatIfNotEmpty("{%s}"),
                                                                           output.getCurrent(),
                                                                           output.getThreshold(),
                                                                           output.getDelta()))
                                       .collect(Collectors.joining("\n\n"))
                                + "\n";
        } else { // RESOLVED
            evaluationMessage = message.getEvaluationOutputs()
                                       .values()
                                       .stream()
                                       .flatMap(Collection::stream)
                                       .filter((output) -> !output.getLabel().isEmpty())
                                       .map((output) -> output.getLabel().formatIfNotEmpty("{%s}"))
                                       .collect(Collectors.joining("\n\n"));
            if (!evaluationMessage.isEmpty()) {
                evaluationMessage = "\n" + evaluationMessage + "\n";
            }
        }
        messageBody = messageBody.replace("{alert.message}", evaluationMessage);

        // duration text
        long durationMinutes = message.getAlertRule().getEvery().getDuration().toMinutes() * message.getAlertRule().getForTimes();
        messageBody = messageBody.replace("{alert.duration}", message.getStatus() == AlertStatus.ALERTING ? "Lasting for " + durationMinutes + " minutes" : "");

        sendHttp(timeout, serializeRequestBody(messageBody));
    }

    protected void sendHttp(Duration timeout, AbstractHttpEntity bodyEntity) throws IOException {
        RequestConfig requestConfig = RequestConfig.custom()
                                                   .setSocketTimeout((int) timeout.toMillis())
                                                   .setConnectTimeout(1000)
                                                   .build();
        validateTargetUrl(this.props.url);
        try (CloseableHttpClient client = HttpClientBuilder.create()
                                                           .disableRedirectHandling()
                                                           .setDnsResolver(new RestrictedDnsResolver())
                                                           .setDefaultRequestConfig(requestConfig)
                                                           .build()) {
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

    private static void validateTargetUrl(String targetUrl) {
        URL url;
        try {
            url = new URL(targetUrl);
        } catch (MalformedURLException e) {
            throw new Preconditions.InvalidValueException("Invalid URL: %s", targetUrl);
        }

        String protocol = url.getProtocol();
        Preconditions.checkIfTrue("http".equals(protocol) || "https".equals(protocol),
                                  "Invalid URL: %s. Only http and https schemes are supported",
                                  targetUrl);
        Preconditions.checkIfTrue(!StringUtils.isEmpty(url.getHost()), "Invalid URL: %s. Missing host", targetUrl);

        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(url.getHost());
        } catch (UnknownHostException e) {
            throw new Preconditions.InvalidValueException("Invalid URL: %s. Unknown host", targetUrl);
        }
        validateResolvedAddresses(targetUrl, addresses);
    }

    private static void validateResolvedAddresses(String targetUrl, InetAddress[] addresses) {
        for (InetAddress address : addresses) {
            Preconditions.checkIfTrue(!isBlockedAddress(address),
                                      "Invalid URL: %s. Host resolves to a restricted address",
                                      targetUrl);
        }
    }

    private static boolean isBlockedAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
            || address.isLoopbackAddress()
            || address.isLinkLocalAddress()
            || address.isSiteLocalAddress()
            || address.isMulticastAddress()) {
            return true;
        }

        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            return isBlockedIpv4Address(bytes, 0);
        }

        if (address instanceof Inet6Address) {
            int first = bytes[0] & 0xFF;
            return (first & 0xFE) == 0xFC
                   || isBlockedEmbeddedIpv4Address(bytes);
        }

        return false;
    }

    private static boolean isBlockedIpv4Address(byte[] bytes, int offset) {
        int first = bytes[offset] & 0xFF;
        int second = bytes[offset + 1] & 0xFF;

        return first == 0
               || first == 10
               || first == 127
               || (first == 100 && second >= 64 && second <= 127)
               || (first == 169 && second == 254)
               || (first == 172 && second >= 16 && second <= 31)
               || (first == 192 && second == 168)
               || first >= 224;
    }

    private static boolean isBlockedEmbeddedIpv4Address(byte[] bytes) {
        // IPv4-compatible IPv6: ::a.b.c.d
        if (isZero(bytes, 0, 12) && isBlockedIpv4Address(bytes, 12)) {
            return true;
        }

        // IPv4-mapped IPv6: ::ffff:a.b.c.d
        if (isZero(bytes, 0, 10)
            && (bytes[10] & 0xFF) == 0xFF
            && (bytes[11] & 0xFF) == 0xFF
            && isBlockedIpv4Address(bytes, 12)) {
            return true;
        }

        // Well-known NAT64 prefixes: 64:ff9b::/96 and 64:ff9b:1::/48
        if ((bytes[0] & 0xFF) == 0x00
            && (bytes[1] & 0xFF) == 0x64
            && (bytes[2] & 0xFF) == 0xFF
            && (bytes[3] & 0xFF) == 0x9B
            && ((isZero(bytes, 4, 8) && isBlockedIpv4Address(bytes, 12))
                || ((bytes[4] & 0xFF) == 0x00 && (bytes[5] & 0xFF) == 0x01 && isBlockedIpv4Address(bytes, 12)))) {
            return true;
        }

        // 6to4 addresses: 2002:a.b.c.d::/48
        return (bytes[0] & 0xFF) == 0x20
               && (bytes[1] & 0xFF) == 0x02
               && isBlockedIpv4Address(bytes, 2);
    }

    private static boolean isZero(byte[] bytes, int offset, int length) {
        for (int i = offset; i < offset + length; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        return true;
    }

    private static class RestrictedDnsResolver implements DnsResolver {
        @Override
        public InetAddress[] resolve(String host) throws UnknownHostException {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            try {
                validateResolvedAddresses(host, addresses);
            } catch (Preconditions.InvalidValueException e) {
                UnknownHostException unknownHostException = new UnknownHostException(host);
                unknownHostException.initCause(e);
                throw unknownHostException;
            }
            return addresses;
        }
    }

    @Override
    public String toString() {
        return "HttpNotificationChannel{" +
               "url='" + this.props.url + '\'' +
               '}';
    }

    protected AbstractHttpEntity serializeRequestBody(String body) {
        return new StringEntity(body, StandardCharsets.UTF_8);
    }

    private String getURL(NotificationMessage message) {
        String host = notificationProperties.getManagerURL();
        if (StringUtils.isBlank(host)) {
            host = notificationProperties.getManagerHost();
        }
        if (host == null) {
            host = "http://localhost:9897/";
        }
        if (!host.endsWith("/")) {
            host += "/";
        }

        String path = notificationProperties.getDetailPath();
        if (StringUtils.isBlank(path)) {
            path = "web/alerting/record/detail?recordId={id}";
        } else {
            // Make sure the path does not start with '/'
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
        }
        path = path.replace("{ruleId}", message.getAlertRule().getId());
        path = path.replace("{id}", message.getAlertRecordId());

        return host + path;
    }
}
