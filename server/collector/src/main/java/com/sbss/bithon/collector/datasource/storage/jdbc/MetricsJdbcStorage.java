package com.sbss.bithon.collector.datasource.storage.jdbc;

import com.sbss.bithon.collector.datasource.DataSourceSchema;
import com.sbss.bithon.collector.datasource.storage.IMetricReader;
import com.sbss.bithon.collector.datasource.storage.IMetricStorage;
import com.sbss.bithon.collector.datasource.storage.IMetricWriter;
import org.jooq.DSLContext;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 1:37 下午
 */
public class MetricsJdbcStorage implements IMetricStorage {

    private final DSLContext dslContext;

    public MetricsJdbcStorage(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public IMetricWriter createMetricWriter(DataSourceSchema schema) {
        return new MetricWriter(dslContext, schema);
    }

    @Override
    public IMetricReader createMetricReader(DataSourceSchema schema) {
        return new MetricReader(dslContext);
    }
}
