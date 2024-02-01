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

package org.bithon.server.web.service.tracing.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.utils.DbUtils;
import org.bithon.server.storage.tracing.TraceSpan;
import org.bithon.server.web.service.tracing.api.TraceSpanBo;
import org.bithon.server.web.service.tracing.api.TraceTopo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Build application topo for a specific tracing
 *
 * @author frank.chen021@outlook.com
 * @date 24/11/21 7:06 pm
 */
@Data
@Slf4j
public class TraceTopoBuilder {

    static class Instance {
        String application;
        String instanceName;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Instance other = (Instance) o;
            return Objects.equals(application, other.application) && Objects.equals(instanceName, other.instanceName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(application, instanceName);
        }

        public Instance(String application, String instanceName) {
            this.application = application;
            this.instanceName = instanceName;
        }

        public static Instance of(String application, String instanceName) {
            return new Instance(application, instanceName);
        }
    }

    // Use LinkedHashMap to keep the insert order which is the order of calls
    private final Map<String, TraceTopo.Link> links = new LinkedHashMap<>();
    private final Map<String, TraceTopo.Node> nodes = new HashMap<>();

    /**
     * A map from instance to generated node name
     */
    private final Map<Instance, String> nodeNameMap = new HashMap<>();
    private int nodeIndex = 0;
    private int maxHops = -1;

    private String getNodeName(TraceSpan span) {
        return nodeNameMap.computeIfAbsent(Instance.of(span.getAppName(), span.getInstanceName()),
                                           (key) -> "n" + nodeIndex++);
    }

    private TraceTopo.Link addLink(TraceSpan source, TraceSpan target) {
        String srcNodeName = getNodeName(source);
        TraceTopo.Node srcNode = nodes.computeIfAbsent(srcNodeName, n -> new TraceTopo.Node(n, source.getAppName(), source.getInstanceName()));

        String dstNodeName = getNodeName(target);
        TraceTopo.Node dstNode = nodes.get(dstNodeName);
        if (dstNode == null) {
            dstNode = new TraceTopo.Node(dstNodeName, target.getAppName(), target.getInstanceName());
            dstNode.setHop(srcNode.getHop() + 1);
            nodes.put(dstNodeName, dstNode);
        }

        maxHops = Math.max(maxHops, dstNode.getHop());

        TraceTopo.Link link = this.links.computeIfAbsent(srcNodeName + "->" + dstNodeName,
                                                         v -> new TraceTopo.Link(srcNodeName, dstNodeName));
        link.incrCount(source.costTime, !"200".equals(source.status));
        return link;
    }

    @AllArgsConstructor
    static class UserNameDetector {
        private String tag;
        private Function<String, String> nameSupplier;
    }

    static UserNameDetector[] detectors = new UserNameDetector[]{
        new UserNameDetector(Tags.Http.REQUEST_HEADER_PREFIX + "user-agent", UserAgentAnalyzer::shorten),

        // Compatible with old tags
        new UserNameDetector("http.header.User-Agent", UserAgentAnalyzer::shorten),

        new UserNameDetector(Tags.Rpc.REQUEST_META_PREFIX + "user-agent", (userAgent) -> userAgent),
        new UserNameDetector(Tags.Rpc.SYSTEM, (system) -> system + "-client"),
    };

    @SuppressWarnings("unchecked")
    public TraceTopo build(List<? extends TraceSpan> spans) {
        //
        // Step 2. Traverse the tree to get Topo
        //
        for (TraceSpanBo root : (List<TraceSpanBo>) spans) {
            buildLink(root, root.children);
        }

        //
        // Step 3. Create user node if necessary
        //
        TraceSpanBo user = new TraceSpanBo();
        user.setAppName("user");
        for (TraceSpan root : spans) {
            if ("SERVER".equals(root.kind)) {
                for (UserNameDetector detector : detectors) {
                    String val = root.getTag(detector.tag);
                    if (StringUtils.hasText(val)) {
                        user.setAppName(detector.nameSupplier.apply(val));
                        break;
                    }
                }
            } else if (SpanKind.CONSUMER.name().equals(root.kind)) {

                if ("kafka".equals(root.name)) {
                    String topicUri = root.getTag("uri");
                    if (StringUtils.hasText(topicUri)) {
                        try {
                            user.setAppName("kafka");

                            URI uri = new URI(topicUri);
                            user.setInstanceName(uri.getHost() + ":" + uri.getPort());
                        } catch (URISyntaxException ignored) {
                        }
                    }
                } else {
                    user.setAppName("producer");
                    user.setInstanceName(null);
                }
            } else {
                continue;
            }

            user.setStatus(root.getStatus());
            user.setCostTime(root.getCostTime());
            this.addLink(user, root);
        }

        return new TraceTopo(nodes.values(), links.values(), maxHops);
    }

    public static class UserAgentAnalyzer {
        private static final Pattern BROWSER_LEADING_PATTERN = Pattern.compile("^Mozilla/\\d+\\.\\d+ ");
        private static final Pattern OPERATING_SYSTEM_PATTERN = Pattern.compile("(^\\([^)]+\\)) ");

        /**
         * Analyze user agent that from web browser so that the UI shows shorter text
         */
        static String shorten(String userAgent) {
            // If the user agent has a pattern as 'Mozilla/5.0', we treat it as a request from a web browser
            Matcher leadingMatcher = BROWSER_LEADING_PATTERN.matcher(userAgent);
            if (leadingMatcher.find()) {
                userAgent = userAgent.substring(leadingMatcher.end());

                String os = "";
                Matcher osMatcher = OPERATING_SYSTEM_PATTERN.matcher(userAgent);
                if (osMatcher.find()) {
                    os = osMatcher.group(1);
                }

                String[][] browserPatterList = new String[][]{
                    {" OPR/\\d+", "Opera"},
                    {" Safari/\\d+", "Safari"},
                    {" Firefox/\\d+", "Firefox"},
                    {" Edg/\\d++", "Microsoft Edge"},

                    // Must the last one because Edge/Opera also contains this part
                    {" Chrome/\\d++", "Chrome"},
                };
                for (String[] browserPattern : browserPatterList) {
                    if (Pattern.compile(browserPattern[0]).matcher(userAgent).find()) {
                        return browserPattern[1] + os;
                    }
                }
            }
            return userAgent;
        }
    }

    /**
     * Search spans that cross two instances and then create a directed graph.
     * <p> case 1.
     * Spans: SERVER --> CLIENT ---> SERVER
     * Topo: SERVER ---> CLIENT ---> SERVER
     * <p> case 2.
     * Spans: SERVER --> CLIENT
     * Topo: SERVER ---> CLIENT ---> CLIENT_TARGET
     * <p> case 3.
     * Spans: SERVER --> CLIENT(1) ---> CLIENT(2)
     * Topo: SERVER --> CLIENT(2) ---> CLIENT2_TARGET
     * <p> case 4.
     * Span: SERVER ---> PRODUCER
     * Topo: SERVER ---> PRODUCER ---> PRODUCER_TARGET
     * <p> case 5.
     * Span: SERVER ---> PRODUCER ---> CONSUMER
     * Topo: SERVER ---> PRODUCER ---> CONSUMER
     */
    private boolean buildLink(TraceSpanBo parentSpan, List<?> childSpans) {
        // Determine if a tree path has a termination node.
        // Termination node is a node ends with 'CLIENT' or 'PRODUCER' span
        // OR a CLIENT/PRODUCER span that has no child CLIENT/PRODUCER span.
        //
        // Usually, a CLIENT/PRODUCER span is the end of a tree path, but sometimes it's not.
        // For example, for the 'feign' client, its kind is CLIENT, but it might have child CLIENT span like httpclient,
        // So, we don't build a link for this feign client node.
        //
        // However, an httpclient CLIENT span might also have child spans,
        // but none of these spans are CLIENT but just some inner method calls after the CLIENT span,
        // we NEED to build topo for this httpclient CLIENT.
        //
        // To implement the above requirement, the 'hasTermination' is used.
        //
        boolean hasTermination = false;

        //noinspection unchecked
        for (TraceSpanBo childSpan : (List<TraceSpanBo>) childSpans) {

            if (parentSpan.getAppName().equals(childSpan.getAppName())
                && Objects.equals(parentSpan.getInstanceName(), childSpan.getInstanceName())
                && !SpanKind.SERVER.name().equals(childSpan.getKind())
                && !SpanKind.CONSUMER.name().equals(childSpan.getKind())) {
                // The instance of childSpan is the same as the parentSpan,
                // no need to create a link, but just recursively search next level,
                //
                // But if the childSpan is a SERVER/CONSUMER, it means the application itself sends a request/message to itself,
                // in that case, we need to go to the 'else' case
                //
                if (buildLink(parentSpan, childSpan.children)) {
                    hasTermination = true;
                }
            } else {
                // There's a link from the parentSpan to the childSpan,
                // for this parentSpan, it terminates
                hasTermination = true;
                this.addLink(parentSpan, childSpan);

                buildLink(childSpan, childSpan.children);
            }

            // This childSpan is a CLIENT termination.
            // When there are no children, it means the next hop might be another system.
            // So, we need to create a link for this situation
            if (childSpan.children.isEmpty() || !hasTermination) {
                String scheme = "";
                String peer = null;
                if (SpanKind.CLIENT.name().equals(childSpan.getKind())) {
                    peer = childSpan.getTag(Tags.Net.PEER);
                    if (childSpan.getTag(Tags.Http.CLIENT) != null) {
                        scheme = "http";
                    } else if (childSpan.getTag(Tags.Messaging.SYSTEM) != null) {
                        scheme = childSpan.getTag(Tags.Messaging.SYSTEM);
                    } else if (childSpan.getTag(Tags.Database.SYSTEM) != null) {
                        scheme = childSpan.getTag(Tags.Database.SYSTEM);
                        DbUtils.ConnectionString conn = DbUtils.tryParseConnectionString(childSpan.getTag(Tags.Database.CONNECTION_STRING));
                        peer = conn == null ? "Database" : conn.getDbType();
                    } else if (childSpan.getTag(Tags.Database.CONNECTION_STRING) != null) {
                        String connectionString = childSpan.getTag(Tags.Database.CONNECTION_STRING);
                        DbUtils.ConnectionString conn = DbUtils.tryParseConnectionString(childSpan.getTag(connectionString));
                        scheme = conn == null ? "Database" : conn.getDbType();
                        peer = conn == null ? connectionString : conn.getHostAndPort();
                    } else {
                        scheme = "Unknown";
                    }
                } else if (SpanKind.PRODUCER.name().equals(childSpan.getKind())) {
                    scheme = childSpan.getTag(Tags.Messaging.SYSTEM);
                    peer = childSpan.getTag(Tags.Net.PEER);
                    if (scheme == null) {
                        if (childSpan.getTag(Tags.Messaging.KAFKA_TOPIC) != null) {
                            // compatible with old data
                            scheme = "kafka";
                        } else {
                            scheme = "Unknown";
                        }
                    }
                }

                if (peer != null) {
                    TraceSpan next = new TraceSpan();
                    next.setAppName(scheme);
                    next.setInstanceName(peer);
                    this.addLink(childSpan, next);

                    hasTermination = true;
                }
            }
        }

        return hasTermination;
    }
}
