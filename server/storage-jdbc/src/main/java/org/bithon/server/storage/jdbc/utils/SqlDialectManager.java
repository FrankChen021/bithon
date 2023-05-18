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

package org.bithon.server.storage.jdbc.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.component.commons.utils.StringUtils;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Frank Chen
 * @date 18/5/23 10:06 pm
 */
@Component
public class SqlDialectManager {
    protected final Map<String, ISqlDialect> sqlDialectMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public SqlDialectManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ISqlDialect getSqlDialect(DSLContext context) {
        final String name = context.dialect().name().toUpperCase(Locale.ENGLISH);
        return sqlDialectMap.computeIfAbsent(name, (k) -> {
            try {
                return this.objectMapper.readValue(StringUtils.format("{\"type\": \"%s\"}", name), ISqlDialect.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(StringUtils.format("Can't find SqlDialect for %s", name));
            }
        });
    }
}
