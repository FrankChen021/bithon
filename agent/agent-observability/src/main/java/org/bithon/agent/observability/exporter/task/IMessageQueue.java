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

/**
 * @author frankchen
 */
public interface IMessageQueue {

    boolean offer(Object items);

    boolean offer(Object items, Duration wait) throws InterruptedException;

    /**
     * Number of elements in the queue
     */
    long size();

    /**
     * Size limit of this queue
     */
    long capacity();

    /**
     * Wait for at most given milliseconds to take an element from the current queue
     * @param timeout The millisecond that we wait for taking 1 element from this.
     *                If <= 0, no wait
     */
    Object take(long timeout) throws InterruptedException;

    /**
     * Pop the first entry from the queue without waiting
     */
    Object pop();
}
