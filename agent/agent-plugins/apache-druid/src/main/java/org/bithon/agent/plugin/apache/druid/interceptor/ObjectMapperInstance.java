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

package org.bithon.agent.plugin.apache.druid.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.druid.jackson.DefaultObjectMapper;

import java.io.IOException;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/11/5 21:53
 */
public class ObjectMapperInstance {
    private static volatile ObjectMapper objectMapper = null;

    private static ObjectMapper getInstance() {
        if (objectMapper == null) {
            synchronized (QueryLifecycle$Initialize.class) {
                if (objectMapper == null) {
                    objectMapper = new DefaultObjectMapper().copy()
                                                            .configure(SerializationFeature.INDENT_OUTPUT, true);
                }
            }
        }
        return objectMapper;
    }

    public static ObjectNode toTree(Object object) throws IOException {
        try {
            return getInstance().valueToTree(object);
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getCause());
        }
    }

    public static <T> String toString(T object) throws IOException {
        return getInstance().writeValueAsString(object);
    }

    public static <T> T fromTree(ObjectNode newQuery, Class<T> sqlQueryClass) throws IOException {
        return getInstance().treeToValue(newQuery, sqlQueryClass);
    }
}
