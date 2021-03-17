package com.sbss.bithon.server.metric.storage;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sbss.bithon.server.common.utils.datetime.TimeSpan;
import com.sbss.bithon.server.metric.DataSourceSchema;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/11 11:09 上午
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
    //@JsonSubTypes.Type(name = "druid", value = DruidStorageReader.class)
})
public interface IMetricReader {

    List<Map<String, Object>> getMetricValueList(TimeSpan start,
                                                 TimeSpan end,
                                                 DataSourceSchema dataSourceSchema,
                                                 Collection<DimensionCondition> dimensions,
                                                 Collection<String> metrics);

    List<Map<String, Object>> getMetricValueList(String sql);

    List<Map<String, String>> getDimensionValueList(TimeSpan start,
                                                    TimeSpan end,
                                                    DataSourceSchema dataSourceSchema,
                                                    Collection<DimensionCondition> dimensions,
                                                    String dimension);
}
