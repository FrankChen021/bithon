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

package org.bithon.agent.core.dispatcher.task;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/5/14 10:46 上午
 */
public class BlockingQueue implements IMessageQueue {
    private final LinkedBlockingQueue<Object> queue = new LinkedBlockingQueue<>(4096);

    @Override
    public void enqueue(Object item) {
        queue.offer(item);
    }

    @Override
    public Object dequeue(int timeout, TimeUnit unit) {
        try {
            return queue.poll(timeout, unit);
        } catch (InterruptedException ignored) {
            return null;
        }
    }

    @Override
    public void gc() {
    }

    @Override
    public long size() {
        return queue.size();
    }
}
