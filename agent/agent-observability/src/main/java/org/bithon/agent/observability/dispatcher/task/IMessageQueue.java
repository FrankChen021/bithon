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

import java.util.Collection;

/**
 * @author frankchen
 */
public interface IMessageQueue {

    void offer(Object item);

    void offerAll(Collection<Object> items);

    long size();

    /**
     * Wait for at most {timeout} milliseconds to take at most maxElement elements from current queue
     * @param maxElement how many elements to take from this queue
     * @param timeout how long should we wait for taking maxElement elements from this queue
     * @return null if no elements got within specified timeout
     *         A collection that contains at most maxElement elements if maxElement > 1
     *         An object if maxElement == 1
     * @throws InterruptedException
     */
    Object take(int maxElement, long timeout) throws InterruptedException;

    Object take(int maxElement);
}
