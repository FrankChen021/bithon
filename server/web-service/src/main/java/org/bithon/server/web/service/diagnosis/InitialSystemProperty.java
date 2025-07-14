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

package org.bithon.server.web.service.diagnosis;

import one.jfr.JfrReader;
import one.jfr.event.Event;

/**
 * Represents initial system property event from JFR (jdk.InitialSystemProperty)
 * This event captures system properties that were set when the JVM started.
 * 
 * @see https://bestsolution-at.github.io/jfr-doc/openjdk-17.html#jdk.InitialSystemProperty
 * @author frank.chen021@outlook.com
 * @date 2025/1/12
 */
public class InitialSystemProperty extends Event {

    public InitialSystemProperty(JfrReader jfr) {
        super(jfr.getVarlong(), 0, 0);
        this.key = jfr.getString();
        this.value = jfr.getString();
    }

    // Field names should match the actual field names in the jdk.InitialSystemProperty JFR event
    public String key;
    public String value;

    @Override
    public String toString() {
        return "InitialSystemProperty{" +
               "key='" + key + '\'' +
               ", value='" + value + '\'' +
               '}';
    }
} 