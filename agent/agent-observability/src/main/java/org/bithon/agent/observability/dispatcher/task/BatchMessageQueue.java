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

package org.bithon.agent.observability.dispatcher.task;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A wrapper of underlying message queue to take a batch everytime
 *
 * @author Frank Chen
 * @date 19/4/23 10:31 pm
 */
public class BatchMessageQueue implements IThreadSafeQueue {

    private final IThreadSafeQueue delegate;

    private final int batchSize;
    private List<Object> fetched;

    public int getBatchSize() {
        return batchSize;
    }

    public BatchMessageQueue(IThreadSafeQueue delegate, int batchSize) {
        this.delegate = delegate;
        this.batchSize = batchSize;
    }

    @Override
    public boolean offer(Object object) {
        if (object == null) {
            // Treat it as a success
            return true;
        }
        if (!(object instanceof Collection)) {
            // A list is stored in the underlying queue
            object = Collections.singletonList(object);
        } else if (((Collection<?>) object).isEmpty()) {
            // Treat it as a success
            return true;
        }

        return delegate.offer(object);
    }

    @Override
    public boolean offer(Object object, Duration wait) throws InterruptedException {
        return delegate.offer(object, wait);
    }

    @Override
    public long size() {
        return delegate.size();
    }

    @Override
    public long capacity() {
        return delegate.capacity();
    }

    @Override
    public Object take(long timeout) {
        List<Object> target = new ArrayList<>(this.batchSize);

        do {
            long start = System.currentTimeMillis();
            fetch(target, timeout);
            timeout -= System.currentTimeMillis() - start;
        } while (target.size() < batchSize && timeout > 0);

        return target;
    }

    @Override
    public Object pop() {
        return delegate.pop();
    }

    /**
     * Fetch items from underlying queue in given timeout
     */
    @SuppressWarnings("unchecked")
    private void fetch(List<Object> target, long timeout) {
        if (fetched == null) {
            // Only take an item from the queue if the previous 'take' process does not leave any items left
            try {
                fetched = (List<Object>) delegate.take(timeout);
            } catch (InterruptedException ignored) {
            }
        }

        if (fetched == null) {
            // We don't get any item from the queue after the timeout
            return;
        }

        int capacity = this.batchSize - target.size();
        int maxElements = Math.min(capacity, fetched.size());
        for (int i = 0; i < maxElements; i++) {
            target.add(fetched.get(i));
        }

        if (maxElements < fetched.size()) {
            // We still have items left in the 'taken' list, keep it for next time
            fetched = fetched.subList(maxElements, fetched.size());
        } else {
            fetched = null;
        }
    }
}
