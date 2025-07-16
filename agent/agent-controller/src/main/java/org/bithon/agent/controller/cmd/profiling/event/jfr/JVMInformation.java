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

package org.bithon.agent.controller.cmd.profiling.event.jfr;

import one.jfr.JfrReader;
import one.jfr.event.Event;

/**
 * Represents JVM information event from JFR (jdk.JVMInformation).
 * This event captures JVM information when the JVM started.
 * 
 * Label: JVM Information
 * Description: Description of JVM and the Java application
 * Categories: Java Virtual Machine
 * 
 * @see <a href="https://bestsolution-at.github.io/jfr-doc/openjdk-17.html#jdk.JVMInformation">JFR Documentation</a>
 */
public class JVMInformation extends Event {
    
    /**
     * The JVM name (String)
     */
    public String jvmName;
    
    /**
     * The JVM version (String)
     */
    public String jvmVersion;
    
    /**
     * The JVM arguments (String)
     */
    public String jvmArguments;
    
    /**
     * The JVM flags (String)
     */
    public String jvmFlags;
    
    /**
     * The Java arguments (String)
     */
    public String javaArguments;
    
    /**
     * The JVM start time (long, Timestamp)
     */
    public long jvmStartTime;
    
    /**
     * The process ID (long)
     */
    public long pid;
    
    /**
     * Constructor for deserializing jdk.JVMInformation events
     * 
     * Fields are read in the order defined by the JFR specification:
     * startTime, jvmName, jvmVersion, jvmArguments, jvmFlags, javaArguments, jvmStartTime, pid
     * 
     * @param jfr the JfrReader instance used for reading the event data
     */
    public JVMInformation(JfrReader jfr) {
        super(jfr.getVarlong(), 0, 0);  // startTime
        this.jvmName = jfr.getString();
        this.jvmVersion = jfr.getString();
        this.jvmArguments = jfr.getString();
        this.jvmFlags = jfr.getString();
        this.javaArguments = jfr.getString();
        this.jvmStartTime = jfr.getVarlong();
        this.pid = jfr.getVarlong();
    }
    
    @Override
    public String toString() {
        return "JVMInformation{" +
               "jvmName='" + jvmName + '\'' +
               ", jvmVersion='" + jvmVersion + '\'' +
               ", jvmArguments='" + jvmArguments + '\'' +
               ", jvmFlags='" + jvmFlags + '\'' +
               ", javaArguments='" + javaArguments + '\'' +
               ", jvmStartTime=" + jvmStartTime +
               ", pid=" + pid +
               '}';
    }
}
