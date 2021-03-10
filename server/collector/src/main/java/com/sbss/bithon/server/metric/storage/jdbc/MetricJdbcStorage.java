package com.sbss.bithon.server.metric.storage.jdbc;

import com.sbss.bithon.server.metric.DataSourceSchema;
import com.sbss.bithon.server.metric.storage.IMetricReader;
import com.sbss.bithon.server.metric.storage.IMetricStorage;
import com.sbss.bithon.server.metric.storage.IMetricWriter;
import org.jooq.DSLContext;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 1:37 下午
 */
public class MetricJdbcStorage implements IMetricStorage {

    private final DSLContext dslContext;

    public MetricJdbcStorage(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public IMetricWriter createMetricWriter(DataSourceSchema schema) {
        return new MetricJdbcWriter(dslContext, schema);
    }

    @Override
    public IMetricReader createMetricReader(DataSourceSchema schema) {
        return new MetricJdbcReader(dslContext);
    }
}
