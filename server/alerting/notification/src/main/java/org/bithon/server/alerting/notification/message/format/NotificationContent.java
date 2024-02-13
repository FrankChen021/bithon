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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 @author frank.chen021@outlook.com
 * @date 2020-08-27 11:14:09
 */
public class NotificationContent {

    @Getter
    @Setter
    private String title;

    @Getter
    private final List<NotificationTextSection> sections = new ArrayList<>();

    @JsonIgnore
    private final Map<String, NotificationTextSection> sectionMap = new HashMap<>();

    public NotificationContent() {
        sections.add(new NotificationTextSection(""));
    }

    public NotificationTextSection getDefaultSection() {
        return sections.get(0);
    }

    public NotificationTextSection getSection(String section) {
        return sectionMap.computeIfAbsent(section, (key) -> {
            NotificationTextSection n = new NotificationTextSection(section);
            sections.add(n);
            return n;
        });
    }
}
