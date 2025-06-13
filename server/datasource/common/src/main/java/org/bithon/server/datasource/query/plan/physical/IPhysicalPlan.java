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

package org.bithon.server.datasource.query.plan.physical;


import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.server.datasource.query.plan.logical.LogicalAggregate;
import org.bithon.server.datasource.query.plan.logical.LogicalFilter;
import org.bithon.server.datasource.query.result.PipelineQueryResult;

import java.util.concurrent.CompletableFuture;

/**
 * @author frank.chen021@outlook.com
 * @date 4/4/25 3:47 pm
 */
public interface IPhysicalPlan {

    default boolean canPushDownAggregate(LogicalAggregate aggregate) {
        return false;
    }

    default IPhysicalPlan pushDownAggregate(LogicalAggregate aggregate) {
        throw new UnsupportedOperationException("Cannot push down aggregate: " + aggregate);
    }

    default boolean canPushDownFilter() {
        return false;
    }

    default IPhysicalPlan pushDownFilter(LogicalFilter filter) {
        throw new UnsupportedOperationException("Cannot push down filter: " + filter);
    }

    /**
     * Mutate the plan to apply an offset.
     * @param offset can't be null
     */
    default IPhysicalPlan offset(HumanReadableDuration offset) {
        throw new UnsupportedOperationException("Cannot apply offset: " + offset + " to type " + this.getClass().getSimpleName());
    }

    boolean isScalar();

    default String serializeToText() {
        PhysicalPlanSerializer serializer = new PhysicalPlanSerializer();
        serializer(serializer);
        return serializer.getSerializedPlan();
    }

    default void serializer(PhysicalPlanSerializer serializer) {
        serializer.append(this.getClass().getSimpleName());
        serializer.append("\n");
    }

    CompletableFuture<PipelineQueryResult> execute() throws Exception;
}
