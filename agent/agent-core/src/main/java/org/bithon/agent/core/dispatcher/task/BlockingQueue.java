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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/5/14 10:46 上午
 */
public class BlockingQueue implements IMessageQueue {
    private final LinkedBlockingQueue<Object> queue;

    public BlockingQueue() {
        this(4096);
    }

    public BlockingQueue(int queueSize) {
        queue = new LinkedBlockingQueue<>(queueSize);
    }

    @Override
    public void offer(Object item) {
        queue.offer(item);
    }

    @Override
    public void offerAll(Collection<Object> items) {
        queue.addAll(items);
    }

    @Override
    public long size() {
        return queue.size();
    }

    @Override
    public Object take(int maxElement, long timeout) throws InterruptedException {
        if (maxElement == 1) {
            // this is a special case to make it compatible with original behavior
            return queue.poll(timeout, TimeUnit.MILLISECONDS);
        }

        List<Object> returnList = new ArrayList<>(maxElement);

        while (maxElement > 0 && timeout > 0) {
            long start = System.currentTimeMillis();
            Object first = queue.poll(timeout, TimeUnit.MILLISECONDS);
            if (first == null) {
                // we have waited for enough time, return the value directly
                break;
            }

            // add this element to returning
            returnList.add(first);
            if (--maxElement == 0) {
                break;
            }

            // tried to get enough elements from the queue
            int n = queue.drainTo(returnList, maxElement);
            maxElement -= n;

            // calculate how much time left for this operation
            timeout -= System.currentTimeMillis() - start;
        }

        // a case that we get required elements within specified timeout value
        return returnList.isEmpty() ? null : returnList;
    }

    @Override
    public Object take(int maxElement) {
        if (maxElement == 1) {
            return queue.poll();
        }

        List<Object> list = new ArrayList<>(maxElement);
        queue.drainTo(list, maxElement);
        return list.isEmpty() ? null : list;
    }
}
