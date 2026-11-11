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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class StopSignalTest {

    @Test
    public void testWaitReturnsResult() {
        StopSignal signal = new StopSignal();
        Assertions.assertEquals("ok", signal.waitUnlessStopped(() -> "ok"));
        Assertions.assertFalse(signal.isStopped());
    }

    @Test
    public void testNoWaitAfterStop() {
        StopSignal signal = new StopSignal();
        signal.stop();

        AtomicBoolean waited = new AtomicBoolean(false);
        Assertions.assertNull(signal.waitUnlessStopped(() -> {
            waited.set(true);
            return "ok";
        }));
        Assertions.assertFalse(waited.get());
        Assertions.assertTrue(signal.isStopped());
    }

    @Test
    public void testStopInterruptsWait() throws InterruptedException {
        StopSignal signal = new StopSignal();
        CountDownLatch waiting = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>("not returned");
        AtomicBoolean interruptedAfterWait = new AtomicBoolean(true);

        Thread thread = new Thread(() -> {
            result.set(signal.waitUnlessStopped(() -> {
                waiting.countDown();
                Thread.sleep(10_000);
                return "ok";
            }));
            interruptedAfterWait.set(Thread.currentThread().isInterrupted());
        });
        thread.start();
        Assertions.assertTrue(waiting.await(5, TimeUnit.SECONDS));

        long start = System.currentTimeMillis();
        signal.stop();
        thread.join(5000);

        Assertions.assertFalse(thread.isAlive());
        Assertions.assertTrue(System.currentTimeMillis() - start < 5000);
        Assertions.assertNull(result.get());

        // The interrupt status is cleared after the wait
        Assertions.assertFalse(interruptedAfterWait.get());
    }

    @Test
    public void testStopDoesNotInterruptAfterWait() throws InterruptedException {
        StopSignal signal = new StopSignal();
        CountDownLatch waited = new CountDownLatch(1);
        CountDownLatch stopped = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean(true);

        AtomicBoolean stopObserved = new AtomicBoolean(false);

        Thread thread = new Thread(() -> {
            signal.waitUnlessStopped(() -> "ok");
            waited.countDown();
            try {
                // Like sending messages after the wait, which must not be interrupted by stop()
                stopObserved.set(stopped.await(5, TimeUnit.SECONDS));
                interrupted.set(Thread.currentThread().isInterrupted());
            } catch (InterruptedException e) {
                interrupted.set(true);
            }
        });
        thread.start();
        Assertions.assertTrue(waited.await(5, TimeUnit.SECONDS));

        signal.stop();
        stopped.countDown();
        thread.join(5000);

        Assertions.assertFalse(thread.isAlive());
        Assertions.assertTrue(stopObserved.get(), "the thread does not see stop()");
        Assertions.assertFalse(interrupted.get());
        Assertions.assertTrue(signal.isStopped());
    }

    @Test
    public void testStopBeforeBlockingCallInWait() {
        StopSignal signal = new StopSignal();

        // stop() is called after the flag check in waitUnlessStopped but before the blocking call,
        // the blocking call must still be interrupted
        long start = System.currentTimeMillis();
        Object result = signal.waitUnlessStopped(() -> {
            signal.stop();
            Thread.sleep(10_000);
            return "ok";
        });
        Assertions.assertNull(result);
        Assertions.assertTrue(System.currentTimeMillis() - start < 5000);

        // The interrupt status is cleared after the wait
        Assertions.assertFalse(Thread.currentThread().isInterrupted());
    }
}
