/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.core.dispatcher.task;

import com.sbss.bithon.agent.core.utils.SerializationUtils;
import shaded.com.leansoft.bigqueue.BigQueueImpl;
import shaded.com.leansoft.bigqueue.IBigQueue;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author frankchen
 */
public class FileQueueImpl implements IMessageQueue {

    private static final Logger log = LoggerFactory.getLogger(FileQueueImpl.class);

    private IBigQueue queue;

    public FileQueueImpl(String dir, String queueName) throws IOException {
        queue = new BigQueueImpl(dir, queueName);
        if (queue.isEmpty()) {
            queue.removeAll();
        }
    }

    @Override
    public void enqueue(Object item) {
        try {
            queue.enqueue(SerializationUtils.serializeObject(item));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public Object dequeue(int timeout, TimeUnit unit) {
        if (queue.isEmpty()) {
            try {
                Thread.sleep(unit.toMillis(timeout));
            } catch (InterruptedException ignored) {
            }
            return null;
        }
        Object result = null;
        try {
            result = SerializationUtils.deserializeObject(queue.dequeue());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }

    @Override
    public long size() {
        return queue.size();
    }

    @Override
    public void gc() {
        try {
            queue.gc();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
