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

package org.bithon.server.alerting.notification.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.server.alerting.notification.message.ImageMode;
import org.bithon.server.alerting.notification.message.NotificationMessage;
import org.bithon.server.alerting.notification.provider.console.ConsoleNotificationProvider;
import org.bithon.server.alerting.notification.provider.ding.DingNotificationProvider;
import org.bithon.server.alerting.notification.provider.http.HttpNotificationProvider;
import org.bithon.server.alerting.notification.provider.kafka.KafkaNotificationProvider;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/11/30 6:50 下午
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = "ding", value = DingNotificationProvider.class),
    @JsonSubTypes.Type(name = "console", value = ConsoleNotificationProvider.class),
    @JsonSubTypes.Type(name = "http", value = HttpNotificationProvider.class),
    @JsonSubTypes.Type(name = "kafka", value = KafkaNotificationProvider.class)
})
public interface INotificationProvider {
    @JsonIgnore
    default ImageMode getImageMode() {
        return ImageMode.BASE64;
    }

    void notify(NotificationMessage message) throws Exception;
}
