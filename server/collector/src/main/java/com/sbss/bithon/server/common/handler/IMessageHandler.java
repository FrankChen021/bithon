package com.sbss.bithon.server.common.handler;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 4:40 下午
 */
public interface IMessageHandler<MESSAGE> {

    /**
     * get type of messages that are handled by this handler
     */
    String getType();

    void submit(MESSAGE message);
}
