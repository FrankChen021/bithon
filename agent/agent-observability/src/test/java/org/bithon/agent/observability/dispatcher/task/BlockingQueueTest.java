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

import org.bithon.component.commons.utils.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/8/11 17:24
 */
public class BlockingQueueTest {

    static class QueueTestDelegation {
        private final IMessageQueue queue;
        private long elapsed = 0;
        private Object takenObject;

        public QueueTestDelegation(int batchSize) {
            this.queue = new BatchMessageQueue(new BlockingQueue(), batchSize);
        }

        void offer(Object o) {
            queue.offer(o);
        }

        void take(int timeout) {
            long s = System.currentTimeMillis();
            try {
                takenObject = queue.take(timeout);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                elapsed = System.currentTimeMillis() - s;
            }
        }

        List<?> takenObjectAsCollection() {
            return (List<?>) takenObject;
        }
    }

    @Test
    public void testTimeoutWhenTakingNElements() {
        QueueTestDelegation queue = new QueueTestDelegation(5);

        // Take elements from the queue.
        // However, no elements are offered to the queue
        queue.take(2000);

        Assert.assertTrue(queue.elapsed >= 2000);
        Assert.assertTrue(CollectionUtils.isEmpty((Collection<?>) queue.takenObject));
    }

    @Test
    public void testTimeoutAndGotElements() throws InterruptedException {
        QueueTestDelegation queue = new QueueTestDelegation(5);

        int timeout = 3000;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.execute(() -> queue.take(timeout));

        // Wait for a period of time for the first task to begin
        Thread.sleep(500);

        // Offer an item to the queue so that the previous task can take it from the queue
        executor.execute(() -> queue.offer(1));

        // Wait for all tasks to complete
        Thread.sleep(timeout + 1000);

        // Since only one item is offered into the queue, we need to wait for timeout
        Assert.assertTrue(queue.elapsed >= timeout);
        Assert.assertNotNull(queue.takenObject);
        Assert.assertEquals(1, queue.takenObjectAsCollection().size());
        Assert.assertEquals(1, queue.takenObjectAsCollection().get(0));
    }

    @Test
    public void testGotMaxElementsImmediately() {
        QueueTestDelegation queue = new QueueTestDelegation(3);

        queue.offer(1);
        queue.offer(2);
        queue.offer(3);

        int timeout = 3000;
        queue.take(timeout);

        // The 'take' above should return immediately,
        // the elapsed should be a very small value, and we think so that 500ms is a large enough value to compare with
        Assert.assertTrue(queue.elapsed < 500);
        Assert.assertNotNull(queue.takenObject);
        Assert.assertEquals(3, queue.takenObjectAsCollection().size());
        Assert.assertEquals(1, queue.takenObjectAsCollection().get(0));
        Assert.assertEquals(2, queue.takenObjectAsCollection().get(1));
        Assert.assertEquals(3, queue.takenObjectAsCollection().get(2));
    }


    @Test
    public void testWaitForMaxElements() throws InterruptedException {
        QueueTestDelegation queue = new QueueTestDelegation(5);

        int timeout = 5000;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.execute(() -> queue.take(timeout));

        // Offer items slowly so that the 'wait' in the 'take' takes effect
        executor.execute(() -> {
            for (int i = 0; i < 5; i++) {
                try {
                    // wait first to make sure queue.take enters the wait process
                    Thread.sleep(900);
                } catch (InterruptedException ignored) {
                }
                queue.offer(i);
            }
        });

        // wait for tasks to complete
        Thread.sleep(timeout + 1000);

        // Since we offer 1 element every 900ms, the take method should cost at least 4500ms
        Assert.assertTrue(queue.elapsed >= 4500);
        Assert.assertNotNull(queue.takenObject);
        Assert.assertEquals(5, queue.takenObjectAsCollection().size());
        Assert.assertEquals(0, queue.takenObjectAsCollection().get(0));
    }

    @Test
    public void testTakeTwoBatches() throws InterruptedException {
        QueueTestDelegation queue = new QueueTestDelegation(5);

        int timeout = 5000;
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Take the first batch
        executor.execute(() -> queue.take(timeout));

        // Offer items slowly so that the 'wait' in the 'take' takes effect
        executor.execute(() -> {
            queue.offer(Arrays.asList(1, 2, 3));

            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {
            }
            queue.offer(Arrays.asList(4, 5, 6));

            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
            }
            queue.offer(Arrays.asList(7, 8, 9, 10, 11));
        });


        // Wait for tasks to complete
        Thread.sleep(timeout + 1000);

        // It took at least 1500ms to offer 5 elements above
        Assert.assertTrue(queue.elapsed >= 1500);
        Assert.assertNotNull(queue.takenObject);
        Assert.assertEquals(5, queue.takenObjectAsCollection().size());
        Assert.assertEquals(1, queue.takenObjectAsCollection().get(0));
        Assert.assertEquals(2, queue.takenObjectAsCollection().get(1));
        Assert.assertEquals(3, queue.takenObjectAsCollection().get(2));
        Assert.assertEquals(4, queue.takenObjectAsCollection().get(3));
        Assert.assertEquals(5, queue.takenObjectAsCollection().get(4));

        // Take the 2nd batch
        // Since there are 10 items left, another 'take' call will take all these 5 items
        queue.take(timeout);

        // Verify the batch result
        Assert.assertTrue(queue.elapsed < 100);
        Assert.assertNotNull(queue.takenObject);
        Assert.assertEquals(5, queue.takenObjectAsCollection().size());
        Assert.assertEquals(6, queue.takenObjectAsCollection().get(0));
        Assert.assertEquals(7, queue.takenObjectAsCollection().get(1));
        Assert.assertEquals(8, queue.takenObjectAsCollection().get(2));
        Assert.assertEquals(9, queue.takenObjectAsCollection().get(3));
        Assert.assertEquals(10, queue.takenObjectAsCollection().get(4));

        // Only takes the last elements
        queue.take(100);
        Assert.assertNotNull(queue.takenObject);
        Assert.assertEquals(1, queue.takenObjectAsCollection().size());
        Assert.assertEquals(11, queue.takenObjectAsCollection().get(0));

        // No elements left to take
        queue.take(100);
        Assert.assertEquals(0, queue.takenObjectAsCollection().size());
    }
}
