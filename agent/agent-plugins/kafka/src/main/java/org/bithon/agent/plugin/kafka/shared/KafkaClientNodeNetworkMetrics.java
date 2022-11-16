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

package org.bithon.agent.plugin.kafka.shared;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/11/16 14:19
 */
public class KafkaClientNodeNetworkMetrics {
    public String cluster;
    public String clientId;
    public String connectionId;
    public double incomingByteRate;
    public double outgoingByteRate;
    public double requestLatencyAvg;
    public double requestLatencyMax;
    public double requestRate;
    public double requestSizeAvg;
    public double requestSizeMax;
    public double responseRate;
}
