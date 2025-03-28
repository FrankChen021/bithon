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

package org.bithon.server.alerting.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Frank Chen
 * @date 16/3/24 9:02 pm
 */
@Data
@Configuration
@ConfigurationProperties("bithon.alerting.notification")
public class NotificationProperties {
    /**
     * use managerHost instead
     */
    @Deprecated
    private String managerURL;

    private String managerHost = "http://localhost:9897";
    private String detailPath = "web/alerting/record/detail?recordId={id}";
}
