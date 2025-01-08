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

package org.bithon.server.alerting.evaluator.state.redis;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.StringUtils;
import org.joda.time.Period;
import redis.clients.jedis.Protocol;

import java.util.concurrent.TimeUnit;

public class RedisConfig {
    public static class RedisClusterConfig {
        @JsonProperty
        private String nodes;

        // cluster
        @JsonProperty
        private int maxRedirection = 5;

        public String getNodes() {
            return nodes;
        }

        public int getMaxRedirection() {
            return maxRedirection;
        }
    }

    /**
     * Support for long-format and Period style format
     */
    public static class TimeoutConfig {
        private final long milliseconds;

        public TimeoutConfig(String time) {
            this.milliseconds = Period.parse(time).toStandardDuration().getMillis();
        }

        public int getMillisecondsAsInt() {
            if (milliseconds > Integer.MAX_VALUE) {
                throw new RuntimeException(StringUtils.format("Milliseconds %d is out of range of int", milliseconds));
            }
            return (int) milliseconds;
        }
    }

    /**
     * host of a standalone mode redis
     */
    @JsonProperty
    private String host;

    /**
     * port of a standalone mode redis
     */
    @JsonProperty
    @Min(0)
    @Max(65535)
    private int port;

    @JsonProperty
    private HumanReadableDuration timeout = HumanReadableDuration.of(3, TimeUnit.SECONDS);

    /**
     * max connections of redis connection pool
     */
    @JsonProperty
    private int maxTotalConnections = 8;

    /**
     * max idle connections of redis connection pool
     */
    @JsonProperty
    private int maxIdleConnections = 8;

    /**
     * min idle connections of redis connection pool
     */
    @JsonProperty
    private int minIdleConnections = 0;

    @JsonProperty
    private String password;

    @JsonProperty
    @Min(0)
    private int database = Protocol.DEFAULT_DATABASE;

    @JsonProperty
    private RedisClusterConfig cluster;

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public HumanReadableDuration getTimeout() {
        return timeout;
    }

    public int getMaxTotalConnections() {
        return maxTotalConnections;
    }

    public int getMaxIdleConnections() {
        return maxIdleConnections;
    }

    public int getMinIdleConnections() {
        return minIdleConnections;
    }

    public RedisClusterConfig getCluster() {
        return cluster;
    }

    public String getPassword() {
        return password;
    }

    public int getDatabase() {
        return database;
    }
}
