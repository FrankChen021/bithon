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

package org.bithon.agent.plugin.redis.redisson.interceptor;

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.redisson.client.protocol.CommandData;
import org.redisson.command.BatchPromise;

import java.util.concurrent.CompletableFuture;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/11/29 21:09
 */
public class BatchCommandCompletionPromise<T> extends BatchPromise<T> {

    private CompletableFuture<Void> sentPromise;

    public BatchCommandCompletionPromise(BatchPromise<T> delegate, CommandData<?, ?> command) {
        this.sentPromise = delegate.getSentPromise();
        this.sentPromise = sentPromise.whenComplete((result, error) -> {
            try {
                CommandContext commandContext = (CommandContext) ((IBithonObject) command).getInjectedObject();
                if (commandContext != null) {
                    commandContext.complete(error != null);
                }
            } catch (Throwable ignored) {
                // Catch all exceptions to avoid breaking the original promise
            }

            // Relay the result to the original promise
            if (error != null) {
                delegate.completeExceptionally(error);
            } else {
                delegate.complete(null);
            }
        });
    }

    @Override
    public CompletableFuture<Void> getSentPromise() {
        return sentPromise;
    }
}
