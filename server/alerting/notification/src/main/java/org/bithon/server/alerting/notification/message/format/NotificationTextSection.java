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

package org.bithon.server.alerting.notification.message.format;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2020-08-27 11:13:48
 */
@Data
@RequiredArgsConstructor
public class NotificationTextSection {

    @NonNull
    private String title;
    private List<INotificationTextLine> properties = new ArrayList<>();

    public NotificationTextSection add(String name, String value) {
        properties.add(new PropertyTextLine(name, value));
        return this;
    }

    public NotificationTextSection add(INotificationTextLine line) {
        properties.add(line);
        return this;
    }
}
