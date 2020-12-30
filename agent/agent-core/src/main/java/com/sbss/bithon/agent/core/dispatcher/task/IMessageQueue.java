package com.sbss.bithon.agent.core.dispatcher.task;

/**
 * @author frankchen
 */
public interface IMessageQueue {

    void enqueue(Object item);

    Object dequeue();

    void gc();

    long size();
}
