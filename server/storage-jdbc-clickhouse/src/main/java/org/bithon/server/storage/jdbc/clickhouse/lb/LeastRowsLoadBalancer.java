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

import org.bithon.component.commons.utils.CollectionUtils;

import java.util.Collection;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * @author Frank Chen
 * @date 15/11/23 3:42 pm
 */
public class LeastRowsLoadBalancer implements ILoadBalancer {
    private PriorityQueue<Shard> shards = newShards();

    @Override
    public synchronized int nextShard(int writtenRows) {
        Shard shard = shards.poll();
        if (shard != null) {
            shard.writtenRows += writtenRows;
            shards.offer(shard);
            return shard.shardNum;
        }
        return 0;
    }

    @Override
    public void update(Collection<Shard> shards) {
        if (CollectionUtils.isNotEmpty(shards)) {
            PriorityQueue<Shard> newShards = newShards();
            newShards.addAll(shards);
            this.shards = newShards;
        }
    }

    private PriorityQueue<Shard> newShards() {
        return new PriorityQueue<>(Comparator.comparingLong(o -> o.writtenRows));
    }
}
