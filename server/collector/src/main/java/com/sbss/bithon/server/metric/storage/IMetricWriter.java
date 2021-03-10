package com.sbss.bithon.server.metric.storage;

import com.sbss.bithon.server.metric.input.InputRow;

import java.io.IOException;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/2 6:36 下午
 */
public interface IMetricWriter extends AutoCloseable {

    void write(InputRow inputRow) throws IOException;
}
