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

package org.bithon.server.discovery.client;

import org.bithon.component.commons.concurrency.NamedThreadFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/4/10 21:42
 */
public class ServiceInvocationExecutor implements AutoCloseable, Executor {

    private final ExecutorService executorService = Executors.newCachedThreadPool(new NamedThreadFactory("service-invoker", true));

    @Override
    public void close() throws Exception {
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);
    }

    public <T> CompletableFuture<T> submit(Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executorService);
    }

    @Override
    public void execute(Runnable command) {
        executorService.execute(command);
    }
}
