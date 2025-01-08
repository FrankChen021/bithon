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

import org.bithon.component.commons.utils.StringUtils;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPooled;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class RedisClientFactory {
    public static RedisClient create(final RedisConfig config) {
        if (config.getCluster() != null && !StringUtils.isBlank(config.getCluster().getNodes())) {

            Set<HostAndPort> nodes = Arrays.stream(config.getCluster().getNodes().split(","))
                                           .map(String::trim)
                                           .filter((s) -> !StringUtils.isBlank(s))
                                           .map(hostAndPort -> {
                                               int index = hostAndPort.indexOf(':');
                                               if (index <= 0 || index == hostAndPort.length()) {
                                                   throw new RuntimeException(StringUtils.format("Invalid redis cluster configuration: %s", hostAndPort));
                                               }

                                               int port;
                                               try {
                                                   port = Integer.parseInt(hostAndPort.substring(index + 1));
                                               } catch (NumberFormatException e) {
                                                   throw new RuntimeException(StringUtils.format("Invalid port in %s", hostAndPort));
                                               }
                                               if (port <= 0 || port > 65535) {
                                                   throw new RuntimeException(StringUtils.format("Invalid port in %s", hostAndPort));
                                               }

                                               return new HostAndPort(hostAndPort.substring(0, index), port);
                                           }).collect(Collectors.toSet());

            ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
            poolConfig.setMaxTotal(config.getMaxTotalConnections());
            poolConfig.setMaxIdle(config.getMaxIdleConnections());
            poolConfig.setMinIdle(config.getMinIdleConnections());

            JedisCluster cluster;
            if (config.getPassword() != null) {
                cluster = new JedisCluster(
                    nodes,
                    (int) config.getTimeout().getDuration().toMillis(), //connection timeout
                    (int) config.getTimeout().getDuration().toMillis(), //read timeout
                    config.getCluster().getMaxRedirection(),
                    config.getPassword(),
                    poolConfig
                );
            } else {
                cluster = new JedisCluster(
                    nodes,
                    (int) config.getTimeout().getDuration().toMillis(), //connection timeout and read timeout
                    config.getCluster().getMaxRedirection(),
                    poolConfig
                );
            }

            return new RedisClient(cluster);

        } else {
            if (StringUtils.isBlank(config.getHost())) {
                throw new RuntimeException("Invalid redis configuration. no redis server or cluster configured.");
            }

            return new RedisClient(
                new JedisPooled(new HostAndPort(config.getHost(), config.getPort()),
                                DefaultJedisClientConfig.builder()
                                                        .timeoutMillis((int) config.getTimeout().getDuration().toMillis())
                                                        .database(config.getDatabase())
                                                        .password(config.getPassword())
                                                        .build()
                )
            );
        }
    }
}
