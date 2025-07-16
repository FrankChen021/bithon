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
 * Represents initial environment variable event from JFR (jdk.InitialEnvironmentVariable).
 * This event captures the environment variables that were set when the JVM started.
 * 
 * @see <a href="https://bestsolution-at.github.io/jfr-doc/openjdk-17.html#jdk.InitialEnvironmentVariable">JFR Documentation</a>
 */
public class InitialEnvironmentVariable extends Event {
    
    /**
     * The environment variable name (e.g., "PATH", "JAVA_HOME")
     */
    public String key;
    
    /**
     * The environment variable value
     */
    public String value;
    
    /**
     * Constructor for deserializing jdk.InitialEnvironmentVariable events
     * 
     * @param jfr the JfrReader instance used for reading the event data
     */
    public InitialEnvironmentVariable(JfrReader jfr) {
        super(jfr.getVarlong(), 0, 0);
        this.key = jfr.getString();
        this.value = jfr.getString();
    }
    
    @Override
    public String toString() {
        return "InitialEnvironmentVariable{" +
               "key='" + key + '\'' +
               ", value='" + value + '\'' +
               '}';
    }
}
