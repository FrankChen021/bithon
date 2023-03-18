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

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/8/11 17:24
 */
public class BlockingQueueTest {

    static class QueueTestDelegation {
        private final BlockingQueue queue = new BlockingQueue();
        private long elapsed = 0;
        private Object takenObject;

        void offer(Object o) {
            queue.offer(o);
        }

        void take(int maxElement, int timeout) {
            long s = System.currentTimeMillis();
            try {
                takenObject = queue.take(maxElement, timeout);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            elapsed = System.currentTimeMillis() - s;
        }

        List<?> takenObjectAsCollection() {
            return (List<?>) takenObject;
        }
    }

    @Test
    public void testTimeoutWhenTakingNElements() {
        QueueTestDelegation queue = new QueueTestDelegation();

        queue.take(5, 2000);

        Assert.assertTrue(queue.elapsed >= 2000);
        Assert.assertNull(queue.takenObject);
    }

    @Test
    public void testTimeoutAndGotElements() throws InterruptedException {
        QueueTestDelegation queue = new QueueTestDelegation();

        int timeout = 3000;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.execute(() -> queue.take(5, timeout));

        // wait for a period time for the first task to begin
        Thread.sleep(500);
        executor.execute(() -> queue.offer(1));

        // wait for task to complete
        Thread.sleep(timeout + 1000);

        Assert.assertTrue(queue.elapsed >= timeout);
        Assert.assertNotNull(queue.takenObject);
        Assert.assertEquals(1, queue.takenObjectAsCollection().size());
        Assert.assertEquals(1, queue.takenObjectAsCollection().get(0));
    }

    @Test
    public void testGotMaxElementsImmediately() {
        QueueTestDelegation queue = new QueueTestDelegation();

        queue.offer(1);
        queue.offer(2);
        queue.offer(3);

        int timeout = 3000;
        queue.take(3, timeout);

        // should return immediately so elapsed should be a very small value
        // so 500ms is large enough value to compare
        Assert.assertTrue(queue.elapsed < 500);
        Assert.assertNotNull(queue.takenObject);
        Assert.assertEquals(3, queue.takenObjectAsCollection().size());
        Assert.assertEquals(1, queue.takenObjectAsCollection().get(0));
        Assert.assertEquals(2, queue.takenObjectAsCollection().get(1));
        Assert.assertEquals(3, queue.takenObjectAsCollection().get(2));
    }


    @Test
    public void testWaitForMaxElements() throws InterruptedException {
        QueueTestDelegation queue = new QueueTestDelegation();

        int timeout = 5000;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.execute(() -> queue.take(5, timeout));

        // wait for a period time for the first task to begin
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

        // wait for task to complete
        Thread.sleep(timeout + 1000);

        // Since we offer 1 element every 900ms, the take method should cost at least 4500ms
        Assert.assertTrue(queue.elapsed >= 4500);
        Assert.assertNotNull(queue.takenObject);
        Assert.assertEquals(5, queue.takenObjectAsCollection().size());
        Assert.assertEquals(0, queue.takenObjectAsCollection().get(0));
    }
}
