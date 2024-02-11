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

package org.bithon.server.alerting.notification.api;

import org.bithon.server.alerting.notification.message.NotificationMessage;
import org.bithon.server.discovery.declaration.DiscoverableService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * This interface is declared
 * as {@link DiscoverableService} so that the evaluator module can find it by using service discovery
 *
 * @author frank.chen021@outlook.com
 * @date 2023/12/22 16:59
 */
@DiscoverableService(name = "alerting-notification-api")
public interface INotificationApi {

    /**
     * @param name The name of channel
     */
    @PostMapping("/alerting/api/alert/notify")
    void notify(@RequestParam("name") String name,
                @RequestBody NotificationMessage message) throws Exception;
}
