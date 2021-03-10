package com.sbss.bithon.server.events.storage;

import com.sbss.bithon.server.events.collector.EventMessage;

import java.io.IOException;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 4:17 下午
 */
public interface IEventWriter extends AutoCloseable {

    void write(EventMessage eventMessage) throws IOException;
}
