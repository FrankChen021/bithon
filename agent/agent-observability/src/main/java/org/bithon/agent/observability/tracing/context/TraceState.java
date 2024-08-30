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

package org.bithon.agent.observability.tracing.context;

import org.bithon.component.commons.utils.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Customized tracing information passed between services.
 * It maps to the W3C tracestate header.
 *
 * @author frank.chen021@outlook.com
 * @date 2024/8/28 22:46
 */
public class TraceState {
    private static final TraceState EMPTY = new TraceState("", Collections.emptyMap());

    private String stateText;
    private final Map<String, String> kv;

    public static TraceState deserialize(String stateText) {
        if (StringUtils.isEmpty(stateText)) {
            return EMPTY;
        } else {
            return new TraceState(stateText,
                                  StringUtils.extractKeyValueParis(stateText,
                                                                   ",",
                                                                   "=",
                                                                   new LinkedHashMap<>()));
        }
    }

    private TraceState(String stateText, Map<String, String> kv) {
        this.stateText = stateText;
        this.kv = kv;
    }

    public boolean isEmpty() {
        return kv.isEmpty();
    }

    public String get(String key) {
        return kv.get(key);
    }

    public void set(String key, String val) {
        if (val == null) {
            kv.remove(key);
        } else {
            kv.put(key, val);
        }
        this.stateText = serialize();
    }

    @Override
    public String toString() {
        return stateText;
    }

    private String serialize() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : kv.entrySet()) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    public int getInt(String key, int defaultValue) {
        String val = this.kv.get(key);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
