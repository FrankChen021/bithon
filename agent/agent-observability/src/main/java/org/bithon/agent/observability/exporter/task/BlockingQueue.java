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

package org.bithon.agent.observability.exporter.task;

import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/5/14 10:46 上午
 */
public class BlockingQueue implements IMessageQueue {
    private final LinkedBlockingQueue<Object> queue;
    private final int capacity;

    public BlockingQueue() {
        this(4096);
    }

    public BlockingQueue(int capacity) {
        this.queue = new LinkedBlockingQueue<>(capacity);
        this.capacity = capacity;
    }

    @Override
    public boolean offer(Object object) {
        return queue.offer(object);
    }

    @Override
    public boolean offer(Object object, Duration wait) throws InterruptedException {
        return queue.offer(object, wait.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public long size() {
        return queue.size();
    }

    @Override
    public long capacity() {
        return this.capacity;
    }

    @Override
    public Object take(long timeout) throws InterruptedException {
        return timeout <= 0 ? queue.poll() : queue.poll(timeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public Object pop() {
        return queue.poll();
    }
}
