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

package org.bithon.agent.observability.metric.model.generator;

import java.util.function.BiFunction;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/2/13 20:23
 */
public interface IAggregate<T> {
    /**
     * TODO:
     * change the return type to T
     * so that it matches the BiFunction<T,T,T> signature which can be directly used as the 3nd parameter of
     * {@link java.util.Map#merge(Object, Object, BiFunction)}
     *
     * @param prev
     * @param now
     */
    void aggregate(T prev, T now);
}
