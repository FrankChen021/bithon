package com.sbss.bithon.server.event.storage;

import com.sbss.bithon.server.event.handler.EventMessage;

import java.io.IOException;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 4:17 下午
 */
public interface IEventWriter extends AutoCloseable {

    void write(EventMessage eventMessage) throws IOException;
}
