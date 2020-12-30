package com.sbss.bithon.agent.dispatcher.metrics.counter;

import java.util.List;

/**
 * Description : 插件的计数器(统计器)
 * <br>Date: 17/10/30
 *
 * @author 马至远
 */
public interface IAgentCounter {
    /**
     * 将数据add至Counter
     *
     * @param o 需要add的数据
     */
    void add(Object o);

    /**
     * 当前计数器是否为空(即是否需要进行数据发送, 用于提高发送队列效率)
     *
     * @return true-空counter, 无需发送 ; false-需要发送
     */
    boolean isEmpty();

    /**
     * 构建并返回当前Counter中所有数据, 传入参数用于entity构建, 只需要实现构建过程, 请勿在逻辑中主动调用此方法
     *
     * @param interval  这次统计的时间窗口大小, 以秒为单位
     * @param appName   应用名称
     * @param ipAddress 应用ip
     * @param port      应用端口
     * @return 采集数据
     */
    List<?> buildAndGetThriftEntities(int interval, String appName, String ipAddress, int port);
}
