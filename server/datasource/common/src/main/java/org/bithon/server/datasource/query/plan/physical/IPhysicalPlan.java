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


import org.bithon.server.datasource.query.result.PipelineQueryResult;

import java.util.concurrent.CompletableFuture;

/**
 * @author frank.chen021@outlook.com
 * @date 4/4/25 3:47 pm
 */
public interface IPhysicalPlan {

    boolean isScalar();

    default String serializeToText() {
        StringBuilder builder = new StringBuilder();
        serializer(builder);
        return builder.toString();
    }

    default void serializer(StringBuilder builder) {
        builder.append(this.getClass().getSimpleName());
        builder.append("\n");
    }

    CompletableFuture<PipelineQueryResult> execute() throws Exception;
}
