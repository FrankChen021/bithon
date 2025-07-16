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
 * https://bestsolution-at.github.io/jfr-doc/openjdk-17.html#jdk.CPUInformation
 */
public class CPUInformation extends Event {
    public CPUInformation(JfrReader jfr) {
        super(jfr.getVarlong(), 0, 0);
        this.cpu = jfr.getString();
        this.description = jfr.getString();
        this.sockets = jfr.getVarint();
        this.cores = jfr.getVarint();
        this.hwThreads = jfr.getVarint();
    }

    // Field names should match the actual field names in the jdk.CPUInformation JFR event
    public String cpu;
    public String description;
    public int sockets;
    public int cores;
    public int hwThreads;

    @Override
    public String toString() {
        return "CPUInformation{" +
               "cpu='" + cpu + '\'' +
               ", description='" + description + '\'' +
               ", sockets=" + sockets +
               ", cores=" + cores +
               ", hwThreads=" + hwThreads +
               '}';
    }
}
