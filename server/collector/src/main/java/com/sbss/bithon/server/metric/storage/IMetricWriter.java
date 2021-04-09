package com.sbss.bithon.server.metric.storage;

import com.sbss.bithon.server.metric.input.InputRow;
import com.sbss.bithon.server.metric.input.MetricSet;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/2 6:36 下午
 */
public interface IMetricWriter extends AutoCloseable {

    void write(InputRow inputRow) throws IOException;

    void write(List<InputRow> inputRowList) throws IOException;

    void write(Collection<MetricSet> metricSetList) throws IOException;
}
