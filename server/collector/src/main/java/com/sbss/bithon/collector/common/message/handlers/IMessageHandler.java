package com.sbss.bithon.collector.common.message.handlers;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 4:40 下午
 */
public interface IMessageHandler<T> {

    void submit(T message);
}
