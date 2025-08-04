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

package org.bithon.agent.controller.cmd.profiling.jfr.event;

import one.jfr.JfrReader;
import one.jfr.event.Event;

/**
 * Represents OS information event from JFR (jdk.OSInformation).
 * This event captures operating system information when the JVM started.
 *
 * @see <a href="https://bestsolution-at.github.io/jfr-doc/openjdk-17.html#jdk.OSInformation">JFR Documentation</a>
 */
public class OSInformation extends Event {
    /**
     * The operating system version
     */
    public String osVersion;

    /**
     * Constructor for deserializing jdk.OSInformation events
     *
     * @param jfr the JfrReader instance used for reading the event data
     */
    public OSInformation(JfrReader jfr) {
        super(jfr.getVarlong(), 0, 0);
        this.osVersion = jfr.getString();
    }

    @Override
    public String toString() {
        return "OSInformation{" +
               "osVersion='" + osVersion + '\'' +
               '}';
    }
}
