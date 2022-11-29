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


package org.bithon.agent.plugin.kafka.producer.metrics;

/**
 * @author frankchen
 */
public class KafkaProducerClientMetrics {
    public String cluster;
    public String clientId;
    public Measurement measurement;

    public static class Measurement {
        public double batchSizeAvg;
        public double batchSizeMax;
        public double bufferAvailableBytes;
        public double bufferExhaustedRate;
        public double bufferTotalBytes;
        public double bufferpoolWaitRatio;
        public double compressionRateAvg;
        public double metadataAge;
        public double produceThrottleTimeAvg;
        public double produceThrottleTimeMax;
        public double recordErrorRate;
        public double recordQueueTimeAvg;
        public double recordQueueTimeMax;
        public double recordRetryRate;
        public double recordSendRate;
        public double recordSizeAvg;
        public double recordSizeMax;
        public double recordsPerRequestAvg;
        public double requestLatencyAvg;
        public double requestLatencyMax;
        public double requestsInFlight;
        public double waitingThreads;
        public double connectionCloseRate;
        public double connectionCount;
        public double connectionCreationRate;
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
}

