package com.sbss.bithon.collector.datasource.storage;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sbss.bithon.collector.datasource.DataSourceSchema;

import java.io.IOException;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/1 4:53 下午
 * <p>
 * use ObjectMapper.registerSubTypes to register type of sub-class for deserialization
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface IMetricStorage {

    IMetricWriter createMetricWriter(DataSourceSchema schema) throws IOException;

    IMetricReader createMetricReader(DataSourceSchema schema);
}
