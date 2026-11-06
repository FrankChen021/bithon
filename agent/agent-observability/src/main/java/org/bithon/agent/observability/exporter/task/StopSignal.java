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

/**
 * A stop flag that also wakes up a thread waiting in {@link #waitUnlessStopped(Wait)}.
 * <p>
 * The waiting thread is interrupted only while it's in the wait, but never after it leaves the wait,
 * so that the code after the wait, like sending messages, is not affected by {@link #stop()}.
 *
 * @author frankchen
 */
class StopSignal {

    @FunctionalInterface
    interface Wait<T> {
        T run() throws InterruptedException;
    }

    private volatile boolean isStopped = false;

    /**
     * The thread that is in {@link #waitUnlessStopped(Wait)}. Guarded by 'this'.
     */
    private Thread waitingThread;

    boolean isStopped() {
        return isStopped;
    }

    /**
     * Run the given wait, which is interrupted if {@link #stop()} is called during the wait.
     *
     * @return the result of the wait, or null if it's stopped before or during the wait
     */
    <T> T waitUnlessStopped(Wait<T> wait) {
        synchronized (this) {
            // Check the flag under the lock, so that a stop() before this point is not missed
            if (isStopped) {
                return null;
            }
            waitingThread = Thread.currentThread();
        }
        try {
            return wait.run();
        } catch (InterruptedException ignored) {
            return null;
        } finally {
            synchronized (this) {
                waitingThread = null;

                // Clear the interrupt status that might be set by stop(),
                // stop() can't interrupt this thread any more after it leaves the lock
                Thread.interrupted();
            }
        }
    }

    void stop() {
        synchronized (this) {
            isStopped = true;
            if (waitingThread != null) {
                waitingThread.interrupt();
            }
        }
    }
}
