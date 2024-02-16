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

package org.bithon.server.alerting.evaluator.storage.redis;

import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.UnifiedJedis;

import java.time.Duration;

public class RedisClient {
    private final UnifiedJedis jedis;

    RedisClient(JedisPooled pool) {
        this.jedis = pool;
    }

    RedisClient(JedisCluster cluster) {
        this.jedis = cluster;
    }

    public void expire(String key, Duration duration) {
        jedis.expire(key, duration.getSeconds());
    }

    public long increment(String key) {
        return jedis.incr(key);
    }

    public Duration getExpire(String silence) {
        long timestamp = jedis.expireTime(silence);
        long remaining = timestamp - System.currentTimeMillis() / 1000;
        return Duration.ofSeconds(remaining);
    }

    public void set(String key, String value, Duration expiration) {
        jedis.set(key, value);
        jedis.expire(key, expiration.getSeconds());
    }

    public String get(String key) {
        return jedis.get(key);
    }

    public boolean setIfAbsent(String key, String val, Duration duration) {
        if (jedis.setnx(key, val) > 0) {
            jedis.expire(key, duration.getSeconds());
            return true;
        } else {
            return false;
        }
    }

    public void delete(String key) {
        jedis.del(key);
    }
}
