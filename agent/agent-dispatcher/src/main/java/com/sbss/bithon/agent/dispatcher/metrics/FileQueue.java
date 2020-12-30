package com.sbss.bithon.agent.dispatcher.metrics;

import com.sbss.bithon.agent.core.util.SerializationUtil;
import shaded.com.leansoft.bigqueue.BigQueueImpl;
import shaded.com.leansoft.bigqueue.IBigQueue;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.io.IOException;

public class FileQueue {

    private static final Logger log = LoggerFactory.getLogger(FileQueue.class);

    private String name;

    private IBigQueue queue;

    public FileQueue(String dir, String name) throws IOException {
        queue = new BigQueueImpl(dir, name);
        this.name = name;
        if (queue.isEmpty()) {
            queue.removeAll();
        }
    }

    public void product(Object item) {
        try {
            queue.enqueue(SerializationUtil.serializeObject(item));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public Object consume() {
        if (queue.isEmpty())
            return null;
        Object result = null;
        try {
            result = SerializationUtil.deserializeObject(queue.dequeue());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }

    public long size() {
        return queue.size();
    }

    public void gc() {
        try {
            queue.gc();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public IBigQueue getQueue() {
        return queue;
    }

    public void setQueue(IBigQueue queue) {
        this.queue = queue;
    }
}
