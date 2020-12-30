package com.sbss.bithon.agent.core.dispatcher.task;

import com.sbss.bithon.agent.core.utils.SerializationUtils;
import shaded.com.leansoft.bigqueue.BigQueueImpl;
import shaded.com.leansoft.bigqueue.IBigQueue;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.io.IOException;

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
    public Object dequeue() {
        if (queue.isEmpty()) {
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
