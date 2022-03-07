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

package org.bithon.server.tracing.sink;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.common.service.UriNormalizer;
import org.bithon.server.common.utils.MiscUtils;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/4 8:28 下午
 */
@Data
public class TraceSpan {
    public String appName;
    public String instanceName;
    public String traceId;
    public String spanId;
    public String kind;
    public String parentSpanId;
    public String parentApplication;
    public Map<String, String> tags;
    public long costTime;
    public long startTime;
    public long endTime;
    public String name;
    public String clazz;
    public String method;
    public String status = "";
    public String normalizeUri = "";

    @JsonIgnore
    private Map<String, String> uriParameters;

    public Map<String, String> getURIParameters() {
        if (uriParameters == null) {
            String uri = tags.get("http.uri");
            if (StringUtils.isBlank(uri)) {
                // compatibility
                uri = tags.get("uri");
            }
            uriParameters = MiscUtils.parseURLParameters(uri);
        }
        return uriParameters;
    }

    public boolean containsTag(String name) {
        return tags.containsKey(name);
    }

    public String getTag(String name) {
        return tags.get(name);
    }

    public void setTag(String name, String value) {
        try {
            this.tags.put(name, value);
        } catch (UnsupportedOperationException e) {
            this.tags = new HashMap<>(this.tags);
            this.tags.put(name, value);
        }
    }

    /**
     * flatten some properties in tags.
     * SHOULD be called AFTER all properties have been set
     */
    public void flatten(UriNormalizer uriNormalizer) {
        if ("SERVER".equals(this.kind) && !CollectionUtils.isEmpty(this.tags)) {
            this.status = tags.getOrDefault("http.status", "");
            if ("".equals(this.status)) {
                // compatibility
                this.status = tags.getOrDefault("status", "");
            }

            String uri = tags.getOrDefault("http.uri", "");
            if ("".equals(uri)) {
                // compatibility
                uri = tags.getOrDefault("uri", "");
            }
            this.normalizeUri = uriNormalizer.normalize(this.appName, uri).getUri();
        }
    }

    @Override
    public String toString() {
        return "TraceSpan{" +
               "appName='" + appName + '\'' +
               ", instanceName='" + instanceName + '\'' +
               ", traceId='" + traceId + '\'' +
               ", spanId='" + spanId + '\'' +
               ", kind='" + kind + '\'' +
               ", parentSpanId='" + parentSpanId + '\'' +
               ", parentApplication='" + parentApplication + '\'' +
               ", tags=" + tags +
               ", costTime=" + costTime +
               ", startTime=" + startTime +
               ", endTime=" + endTime +
               ", name='" + name + '\'' +
               ", clazz='" + clazz + '\'' +
               ", method='" + method + '\'' +
               '}';
    }
}
