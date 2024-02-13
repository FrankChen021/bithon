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

import lombok.Getter;

import java.util.Objects;

/**
 * @author frankchen
 * @date 2020-08-27 11:03:24
 */
public class PropertyTextLine implements INotificationTextLine {

    @Getter
    private final String name;

    @Getter
    private final String value;

    public PropertyTextLine(String name, String value) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);
        this.name = name;
        this.value = value;
    }

    @Override
    public String getType() {
        return "property";
    }
}
