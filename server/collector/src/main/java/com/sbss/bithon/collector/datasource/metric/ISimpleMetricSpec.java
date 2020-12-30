package com.sbss.bithon.collector.datasource.metric;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/14 3:30 下午
 */
public interface ISimpleMetricSpec extends IMetricSpec {

    /**
     * 消息中的字段名
     */
    String getField();
}
