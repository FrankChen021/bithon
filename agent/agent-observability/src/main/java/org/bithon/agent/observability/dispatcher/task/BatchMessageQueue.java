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
public class BatchMessageQueue implements IMessageQueue {

    private final IMessageQueue delegate;

    private final int batchSize;
    private List<Object> taken;

    public int getBatchSize() {
        return batchSize;
    }

    public BatchMessageQueue(IMessageQueue delegate, int batchSize) {
        this.delegate = delegate;
        this.batchSize = batchSize;
    }

    @Override
    public boolean offer(Object object) {
        if (object == null) {
            return false;
        }
        if (!(object instanceof Collection)) {
            // A list is stored in the underlying queue
            object = Collections.singletonList(object);
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
    public Object take(long timeout) {
        List<Object> batch = new ArrayList<>(this.batchSize);

        do {
            long start = System.currentTimeMillis();
            fill(batch, timeout);
            timeout -= System.currentTimeMillis() - start;
        } while (batch.size() < batchSize && timeout > 0);

        return batch;
    }

    @SuppressWarnings("unchecked")
    private void fill(List<Object> thisBatch, long timeout) {
        if (taken == null) {
            try {
                taken = (List<Object>) delegate.take(timeout);
            } catch (InterruptedException ignored) {
            }
        }

        if (taken == null) {
            return;
        }

        int capacity = this.batchSize - thisBatch.size();
        int maxElements = Math.min(capacity, taken.size());
        for (int i = 0; i < maxElements; i++) {
            thisBatch.add(taken.get(i));
        }

        if (maxElements < taken.size()) {
            taken = taken.subList(maxElements, taken.size());
        } else {
            taken = null;
        }
    }
}
