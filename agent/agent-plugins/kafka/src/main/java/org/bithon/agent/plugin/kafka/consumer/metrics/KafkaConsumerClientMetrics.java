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

package org.bithon.agent.plugin.kafka.consumer.metrics;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/11/16 14:10
 */
public class KafkaConsumerClientMetrics {

    public String cluster; 
    public String clientId; 
    public String groupName; 
    public KafkaConsumerCoordinatorMetrics coordinator;
    public KafkaConsumerFetcherMetrics fetcher;
    public double connectionCloseRate; 
    public double connectionCount; 
    public double connectionCreateRate; 
    public double incomingByteRate; 
    public double ioRatio; 
    public double ioTimeNsAvg; 
    public double ioWaitRatio; 
    public double ioWaitTimeNsAvg; 
    public double networkIoRate; 
    public double outgoingByteRate; 
    public double requestRate; 
    public double requestSizeMax; 
    public double requestSizeAvg; 
    public double responseRate; 
    public double selectRate; 
}
