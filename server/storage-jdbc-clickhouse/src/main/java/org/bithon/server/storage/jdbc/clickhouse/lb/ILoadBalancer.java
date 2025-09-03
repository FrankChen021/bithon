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

/**
 * @author Frank Chen
 * @date 15/11/23 3:41 pm
 */
public interface ILoadBalancer {
    LoadBalancerStrategy getStrategyName();

    int nextShard(int writtenRows);

    void update(Collection<Shard> shards);
}
