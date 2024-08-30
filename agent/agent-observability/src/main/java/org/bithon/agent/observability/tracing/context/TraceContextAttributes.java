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
 * @author frank.chen021@outlook.com
 * @date 2024/8/28 22:46
 */
public class TraceContextAttributes {
    private static final TraceContextAttributes EMPTY = new TraceContextAttributes("", Collections.emptyMap());

    private String attributes;
    private final Map<String, String> map;

    public static TraceContextAttributes deserialize(String attributeText) {
        if (StringUtils.isEmpty(attributeText)) {
            return EMPTY;
        } else {
            return new TraceContextAttributes(attributeText,
                                              StringUtils.extractKeyValueParis(attributeText,
                                                                               ",",
                                                                               "=",
                                                                               new LinkedHashMap<>()));
        }
    }

    private TraceContextAttributes(String attributeText, Map<String, String> map) {
        this.attributes = attributeText;
        this.map = map;
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public String get(String key) {
        return map.get(key);
    }

    public void set(String key, String val) {
        if (val == null) {
            map.remove(key);
        } else {
            map.put(key, val);
        }
        this.attributes = serialize();
    }

    @Override
    public String toString() {
        return attributes;
    }

    private String serialize() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    public int getInt(String key, int defaultValue) {
        String val = this.map.get(key);
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
