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

import org.bithon.agent.observability.exporter.config.ExporterConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ExportTaskTest {

    private static final int FLUSH_TIME = 10_000;

    /**
     * Notifies the test each time the send thread starts to wait on the queue
     */
    static class WaitNotifyingQueue implements IThreadSafeQueue {
        private final IThreadSafeQueue delegate = new BlockingQueue();
        private final Semaphore waitEntered = new Semaphore(0);

        @Override
        public boolean offer(Object items) {
            return delegate.offer(items);
        }

        @Override
        public boolean offer(Object items, Duration wait) throws InterruptedException {
            return delegate.offer(items, wait);
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
        public Object take(long timeout) throws InterruptedException {
            if (timeout > 0) {
                waitEntered.release();
            }
            return delegate.take(timeout);
        }

        @Override
        public Object pop() {
            return delegate.pop();
        }

        void awaitWaits(int count) throws InterruptedException {
            Assertions.assertTrue(waitEntered.tryAcquire(count, 5, TimeUnit.SECONDS), "send thread does not wait on the queue");
        }
    }

    /**
     * Records exported messages and whether the send thread was interrupted when sending them
     */
    static class RecordingSender implements Consumer<Object> {
        private final List<Object> exported = Collections.synchronizedList(new ArrayList<>());
        private final List<Boolean> interrupted = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void accept(Object message) {
            interrupted.add(Thread.currentThread().isInterrupted());
            if (message instanceof Collection) {
                exported.addAll((Collection<?>) message);
            } else {
                exported.add(message);
            }
        }

        List<Object> getSortedExported() {
            List<Object> sorted = new ArrayList<>(exported);
            sorted.sort(null);
            return sorted;
        }

        void assertNeverInterrupted() {
            Assertions.assertFalse(interrupted.contains(true), "send thread was interrupted when sending messages");
        }
    }

    private ExporterConfig createConfig(int batchSize) {
        ExporterConfig config = new ExporterConfig();
        config.setBatchSize(batchSize);
        config.setFlushTime(FLUSH_TIME);
        return config;
    }

    private long stopAndMeasure(ExportTask task) {
        long start = System.currentTimeMillis();
        task.stop();
        return System.currentTimeMillis() - start;
    }

    private void testStopInterruptsWait(int batchSize) throws InterruptedException {
        WaitNotifyingQueue queue = new WaitNotifyingQueue();
        RecordingSender sender = new RecordingSender();
        ExportTask task = new ExportTask("test", queue, createConfig(batchSize), sender);
        queue.awaitWaits(1);

        task.accept(1);
        task.accept(2);

        // Each message is taken by one wait, and then the send thread waits for more messages
        queue.awaitWaits(2);

        long elapsed = stopAndMeasure(task);
        Assertions.assertTrue(elapsed < FLUSH_TIME / 2, "stop() took " + elapsed + "ms");

        Assertions.assertEquals(Arrays.asList(1, 2), sender.getSortedExported());
        sender.assertNeverInterrupted();

        // No more messages are accepted after stop
        task.accept(3);
        Assertions.assertFalse(task.canAccept());
        Assertions.assertEquals(Arrays.asList(1, 2), sender.getSortedExported());
    }

    @Test
    public void testStopWithoutBatch() throws InterruptedException {
        testStopInterruptsWait(0);
    }

    @Test
    public void testStopWithBatch() throws InterruptedException {
        // The messages are sent after the wait is interrupted by stop()
        testStopInterruptsWait(100);
    }

    @Test
    public void testStopIdleTask() throws InterruptedException {
        WaitNotifyingQueue queue = new WaitNotifyingQueue();
        RecordingSender sender = new RecordingSender();
        ExportTask task = new ExportTask("test", queue, createConfig(100), sender);
        queue.awaitWaits(1);

        long elapsed = stopAndMeasure(task);
        Assertions.assertTrue(elapsed < FLUSH_TIME / 2, "stop() took " + elapsed + "ms");
        Assertions.assertTrue(sender.getSortedExported().isEmpty());
    }

    /**
     * Blocks the first send until released, and delays the following sends by given milliseconds
     */
    static class BlockingSender extends RecordingSender {
        private final CountDownLatch sending = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final long delayMs;

        BlockingSender() {
            this(0);
        }

        BlockingSender(long delayMs) {
            this.delayMs = delayMs;
        }

        @Override
        public void accept(Object message) {
            try {
                if (sending.getCount() > 0) {
                    sending.countDown();
                    release.await();
                } else if (delayMs > 0) {
                    Thread.sleep(delayMs);
                }
            } catch (InterruptedException e) {
                // Keep the status so that the test can find that the send is interrupted
                Thread.currentThread().interrupt();
            }
            super.accept(message);
        }
    }

    private Thread stopInBackground(ExportTask task) {
        Thread stopThread = new Thread(task::stop, "stop");
        stopThread.start();
        return stopThread;
    }

    private void awaitStopped(ExportTask task) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (task.canAccept() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        Assertions.assertFalse(task.canAccept());
    }

    @Test
    public void testStopWaitsForInProgressSend() throws InterruptedException {
        BlockingSender sender = new BlockingSender();
        ExportTask task = new ExportTask("test", new BlockingQueue(), createConfig(0), sender);

        task.accept(1);
        Assertions.assertTrue(sender.sending.await(5, TimeUnit.SECONDS));

        // These messages stay in the queue while the first one is being sent
        task.accept(2);
        task.accept(3);

        Thread stopThread = stopInBackground(task);

        // stop() must not interrupt the send but wait for it
        stopThread.join(200);
        Assertions.assertTrue(stopThread.isAlive());

        sender.release.countDown();
        stopThread.join(5000);
        Assertions.assertFalse(stopThread.isAlive());

        // The remaining messages are flushed by stop()
        Assertions.assertEquals(Arrays.asList(1, 2, 3), sender.getSortedExported());
        sender.assertNeverInterrupted();
    }

    @Test
    public void testStopFlushesBatchLeftover() throws InterruptedException {
        BlockingSender sender = new BlockingSender();
        ExportTask task = new ExportTask("test", new BlockingQueue(), createConfig(3), sender);

        // The send thread takes the first 3 items, and leaves 2 items for the next batch
        task.accept(Arrays.asList(1, 2, 3, 4, 5));
        Assertions.assertTrue(sender.sending.await(5, TimeUnit.SECONDS));

        // Stop when the first batch is being sent, so that the leftover is flushed by stop()
        Thread stopThread = stopInBackground(task);
        awaitStopped(task);
        sender.release.countDown();
        stopThread.join(5000);
        Assertions.assertFalse(stopThread.isAlive());

        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5), sender.getSortedExported());
        sender.assertNeverInterrupted();
    }

    @Test
    public void testStopTimesOutOnBlockedSend() throws InterruptedException {
        BlockingSender sender = new BlockingSender();
        ExportTask task = new ExportTask("test", new BlockingQueue(), createConfig(0), sender, 300);

        task.accept(1);
        Assertions.assertTrue(sender.sending.await(5, TimeUnit.SECONDS));
        task.accept(2);

        // stop() gives up waiting for the blocked send
        long elapsed = stopAndMeasure(task);
        Assertions.assertTrue(elapsed >= 250 && elapsed < FLUSH_TIME / 2, "stop() took " + elapsed + "ms");
        Assertions.assertTrue(sender.getSortedExported().isEmpty());

        // The blocked send completes after release, and the send thread flushes the remaining message in background
        sender.release.countDown();
        awaitExported(sender, 2);
        Assertions.assertEquals(Arrays.asList(1, 2), sender.getSortedExported());
        sender.assertNeverInterrupted();
    }

    private void awaitExported(RecordingSender sender, int count) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (sender.getSortedExported().size() < count && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        Assertions.assertEquals(count, sender.getSortedExported().size());
    }

    @Test
    public void testStopDoesNotWaitForSlowFlush() throws InterruptedException {
        // Each send after the first one takes 200ms, so flushing all messages would take about 4 seconds
        BlockingSender sender = new BlockingSender(200);
        ExportTask task = new ExportTask("test", new BlockingQueue(), createConfig(0), sender, 500);

        task.accept(1);
        Assertions.assertTrue(sender.sending.await(5, TimeUnit.SECONDS));
        for (int i = 2; i <= 20; i++) {
            task.accept(i);
        }

        Thread stopThread = stopInBackground(task);
        awaitStopped(task);
        sender.release.countDown();

        // stop() returns at the deadline before the flush completes
        stopThread.join(2000);
        Assertions.assertFalse(stopThread.isAlive());
        int exported = sender.getSortedExported().size();
        Assertions.assertTrue(exported >= 1 && exported < 20, "exported " + exported + " messages");

        // The send thread keeps flushing in background
        awaitExported(sender, 20);
        sender.assertNeverInterrupted();
    }

    @Test
    public void testStopDoesNotWaitForBlockedFlush() throws InterruptedException {
        CountDownLatch firstSending = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondSending = new CountDownLatch(1);
        CountDownLatch releaseSecond = new CountDownLatch(1);

        // The first send blocks before stop(), the second one blocks when it's flushed
        RecordingSender sender = new RecordingSender() {
            @Override
            public void accept(Object message) {
                try {
                    if (Integer.valueOf(1).equals(message)) {
                        firstSending.countDown();
                        releaseFirst.await();
                    } else if (Integer.valueOf(2).equals(message)) {
                        secondSending.countDown();
                        releaseSecond.await();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                super.accept(message);
            }
        };
        ExportTask task = new ExportTask("test", new BlockingQueue(), createConfig(0), sender, 500);

        task.accept(1);
        Assertions.assertTrue(firstSending.await(5, TimeUnit.SECONDS));
        task.accept(2);

        Thread stopThread = stopInBackground(task);
        awaitStopped(task);
        releaseFirst.countDown();

        // stop() returns at the deadline although the flush of the second message is blocked
        Assertions.assertTrue(secondSending.await(5, TimeUnit.SECONDS));
        stopThread.join(2000);
        Assertions.assertFalse(stopThread.isAlive());

        releaseSecond.countDown();
        awaitExported(sender, 2);
        sender.assertNeverInterrupted();
    }
}
