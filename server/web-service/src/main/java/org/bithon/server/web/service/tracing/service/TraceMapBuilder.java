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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.tracing.TraceSpan;
import org.bithon.server.web.service.tracing.api.TraceMap;
import org.bithon.server.web.service.tracing.api.TraceSpanBo;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 24/11/21 7:06 pm
 */
@Data
@Slf4j
public class TraceMapBuilder {

    private final Map<String, TraceMap.Link> links = new HashMap<>();

    private final Map<String, TraceMap.Node> nodes = new HashMap<>();
    private final Map<Integer, Integer> levels = new HashMap<>();

    private String getName(TraceSpan span) {
        if (span.getInstanceName() == null) {
            return span.getAppName();
        } else {
            return span.getAppName() + " | " + span.getInstanceName();
        }
    }

    private TraceMap.Link addLink(TraceSpan source, TraceSpan target) {
        String sourceName = getName(source);
        TraceMap.Node srcNode = nodes.get(sourceName);
        if (srcNode == null) {
            srcNode = new TraceMap.Node(sourceName);
            srcNode.setLevelIndex(getAndIncreaseLevelIndex(0));

            nodes.put(sourceName, srcNode);
        }

        String targetName = getName(target);
        TraceMap.Node dstNode = nodes.get(targetName);
        if (dstNode == null) {
            dstNode = new TraceMap.Node(targetName);
            dstNode.setLevel(srcNode.getLevel() + 1);
            dstNode.setLevelIndex(getAndIncreaseLevelIndex(dstNode.getLevel()));

            nodes.put(targetName, dstNode);
        }
        return this.links.computeIfAbsent(sourceName + "->" + targetName,
                                          v -> new TraceMap.Link(sourceName, targetName));
    }

    private int getAndIncreaseLevelIndex(int level) {
        Integer countOfLevel = levels.get(level);
        if (countOfLevel == null) {
            countOfLevel = 0;
        }
        levels.put(level, countOfLevel + 1);
        return countOfLevel;
    }

    public TraceMap buildMap(List<TraceSpan> spans) {
        //
        // build as tree
        //
        Map<String, TraceSpanBo> boMap = spans.stream()
                                              .collect(Collectors.toMap(span -> span.spanId,
                                                                        val -> {
                                                                            TraceSpanBo bo = new TraceSpanBo();
                                                                            BeanUtils.copyProperties(val, bo);
                                                                            return bo;
                                                                        }));

        List<TraceSpanBo> rootSpans = new ArrayList<>();
        for (TraceSpan span : spans) {
            TraceSpanBo bo = boMap.get(span.spanId);
            if (StringUtils.isEmpty(span.parentSpanId)) {
                rootSpans.add(bo);
            } else {
                TraceSpanBo parentSpan = boMap.get(span.parentSpanId);
                if (parentSpan == null) {
                    // this is possible when upstream application A, which is out of monitor of bithon, passes trace context
                    // this an application B which is under monitor of bithon
                    //
                    // In this case, the root span of this trace is application B has parentSpanId,
                    // but obviously, this parentSpanId can't be found in the span collections
                    // So, we add it to the root span
                    //
                    rootSpans.add(bo);

                    // we also clear the parentSpanId field so that the UI can show it correctly
                    // even though doing this is not a very good way
                    span.parentSpanId = "";
                    bo.setParentSpanId("");
                } else {
                    parentSpan.children.add(bo);
                }
            }
        }

        TraceSpanBo user = new TraceSpanBo();
        user.setAppName("user");

        buildMap(user, rootSpans);

        return new TraceMap(nodes.values(), links.values());
    }

    private void buildMap(TraceSpanBo parentSpan, List<TraceSpanBo> childSpans) {
        for (int i = 0, size = childSpans.size(); i < size; i++) {
            TraceSpanBo childSpan = childSpans.get(i);

            if (!parentSpan.getAppName().equals(childSpan.getAppName())
                || !Objects.equals(parentSpan.getInstanceName(), childSpan.getInstanceName())) {

                this.addLink(parentSpan, childSpan).incrCount();

                buildMap(childSpan, childSpan.children);
            } else {
                // the instance of childSpan is the same as the parentSpan
                // no need to create a link, but just recursively search next level
                buildMap(parentSpan, childSpan.children);
            }

            // this childSpan is a CLIENT termination,
            // when there's no children, it means the next hop might be another system.
            // So, we need to create a link for this situation
            if (childSpan.children.size() == 0
                && "CLIENT".equals(childSpan.getKind())
                && childSpan.containsTag("uri")) {

                String uriText = childSpan.getTag("uri");
                try {
                    URI uri = new URI(uriText);
                    TraceSpan next = new TraceSpan();
                    next.setAppName(uri.getScheme());
                    next.setInstanceName(uri.getHost() + ":" + uri.getPort());
                    this.addLink(childSpan, next).incrCount();
                } catch (URISyntaxException e) {
                    log.error("Malformed uri detected in span: {}", childSpan);
                }
            }
        }
    }
}
