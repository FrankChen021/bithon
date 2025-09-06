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

package org.bithon.server.storage.jdbc.clickhouse.lb;


import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author frank.chen021@outlook.com
 * @date 3/9/25 4:40 pm
 */
class RandomLoadBalancer implements ILoadBalancer {
    private List<Shard> shards = Collections.emptyList();

    @Override
    public LoadBalancerStrategy getStrategyName() {
        return LoadBalancerStrategy.RANDOM;
    }

    @Override
    public int nextShard(int writtenRows) {
        synchronized (shards) {
            ThreadLocalRandom.current().nextInt(shards.size());
            return shards.get(0).shardNum;
        }
    }

    @Override
    public void update(Collection<Shard> shards) {
        synchronized (shards) {
            this.shards = List.copyOf(shards);
        }
    }
}
