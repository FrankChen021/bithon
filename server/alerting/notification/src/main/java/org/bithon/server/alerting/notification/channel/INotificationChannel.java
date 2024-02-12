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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.server.alerting.notification.channel.console.ConsoleNotificationChannel;
import org.bithon.server.alerting.notification.channel.ding.DingNotificationChannel;
import org.bithon.server.alerting.notification.channel.http.HttpNotificationChannel;
import org.bithon.server.alerting.notification.channel.kafka.KafkaNotificationChannel;
import org.bithon.server.alerting.notification.message.NotificationMessage;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/11/30 6:50 下午
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = "ding", value = DingNotificationChannel.class),
    @JsonSubTypes.Type(name = "console", value = ConsoleNotificationChannel.class),
    @JsonSubTypes.Type(name = "http", value = HttpNotificationChannel.class),
    @JsonSubTypes.Type(name = "kafka", value = KafkaNotificationChannel.class)
})
public interface INotificationChannel extends AutoCloseable {

    void send(NotificationMessage message) throws Exception;

    default void close() {
    }
}
