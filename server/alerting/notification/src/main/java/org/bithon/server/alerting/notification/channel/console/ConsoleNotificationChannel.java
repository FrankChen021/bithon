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

package org.bithon.server.alerting.notification.channel.console;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.notification.message.NotificationMessage;
import org.bithon.server.alerting.notification.channel.INotificationChannel;

/**
 * Mainly for development
 *
 * @author Frank Chen
 * @date 18/3/22 10:07 PM
 */
@Slf4j
public class ConsoleNotificationChannel implements INotificationChannel {

    @Getter
    @Setter
    @JsonIgnore
    private String name;

    @Override
    public void notify(NotificationMessage message) {
        StringBuilder sb = new StringBuilder(">>>>>>>>>>>ALERT<<<<<<<<<<<<<<<\n");
        sb.append(StringUtils.format("Name: %s\n", message.getAlertRule().getName()));
        message.getConditionEvaluation().forEach((sn, cnd) -> {
            sb.append(StringUtils.format("Condition %s: %s", sn, cnd.getOutputs()));
        });
        log.info(sb.toString());
    }
}
