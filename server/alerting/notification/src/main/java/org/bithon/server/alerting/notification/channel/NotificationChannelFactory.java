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

package org.bithon.server.alerting.notification.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/2/12 12:30
 */
public class NotificationChannelFactory {
    public static INotificationChannel create(String type, String name, Map<String, Object> props, ObjectMapper objectMapper) throws IOException {
        if (CollectionUtils.isEmpty(props)) {
            return create(type, name, "{}", objectMapper);
        } else {
            return create(type, name, objectMapper.writeValueAsString(props), objectMapper);
        }
    }

    public static INotificationChannel create(String type, String name, String props, ObjectMapper objectMapper) throws IOException {
        String sb = '{' +
            StringUtils.format("\"type\":\"%s\",", type) +
            StringUtils.format("\"name\":\"%s\",", name) +
            StringUtils.format("\"props\":%s", props == null ? "null" : props) +
            '}';
        try {
            return objectMapper.readValue(sb, INotificationChannel.class);
        } catch (InvalidTypeIdException e) {
            Matcher matcher = Pattern.compile("known type ids = \\[([^]]+)]").matcher(e.getMessage());
            if (matcher.find()) {
                String knownId = matcher.group(1);
                throw new RuntimeException(StringUtils.format("Unknown type [%s]. Known types are: %s", e.getTypeId(), knownId));
            }
            throw new RuntimeException(StringUtils.format("Unknown type [%s]", e.getTypeId()));
        } catch (ValueInstantiationException e) {
            if (e.getCause() instanceof Preconditions.InvalidValueException) {
                throw (RuntimeException) e.getCause();
            }
            throw e;
        }
    }
}
