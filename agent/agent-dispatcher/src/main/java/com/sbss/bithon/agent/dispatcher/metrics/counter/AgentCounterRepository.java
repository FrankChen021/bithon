package com.sbss.bithon.agent.dispatcher.metrics.counter;

import com.sbss.bithon.agent.dispatcher.metrics.DispatchProcessor;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Description : 用于存放当前所有的Counter, 负责CounterRepository中的数据发送(暂时用同一timer写入队列, 如果以后有别的需求, 可以将写入队列部分抽象出来独立实现)
 * <br>Date: 17/10/30
 *
 * @author 马至远
 */
public class AgentCounterRepository {
    private static final Logger log = LoggerFactory.getLogger(AgentCounterRepository.class);

    /**
     * 仓库的时间窗口大小, (统计频率 秒)
     */
    private static final int INTERVAL = 10;

    /**
     * 当前全部注册counter
     */
    private ConcurrentMap<String, IAgentCounter> counters;

    /**
     * 数据派发器实例
     */
    private DispatchProcessor dispatchProcessor;

    public boolean checkCounterExist(String keyword) {
        for (String counterName : counters.keySet()) {
            if (counterName.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private static class CounterRepositoryHolder {
        static final AgentCounterRepository INSTANCE = new AgentCounterRepository();
    }

    private AgentCounterRepository() {
        this.counters = new ConcurrentHashMap<>();
        this.dispatchProcessor = DispatchProcessor.getInstance();
        init();
    }

    /**
     * 获取CounterRepository实例(单例)
     *
     * @return instance
     */
    public static AgentCounterRepository getInstance() {
        return CounterRepositoryHolder.INSTANCE;
    }

    /**
     * 启动CounterRepository的定时写入工作
     */
    private void init() {
        String appName = dispatchProcessor.getAppName();
        String ipAddress = dispatchProcessor.getIpAddress();

        // 启动定时器, 按照时间窗口interval, 定期向队列写入数据
        new Timer("infra-ac-counter-collector").schedule(new TimerTask() {
            @Override
            public void run() {
                if (dispatchProcessor.ready) {
                    // 遍历counters, 调用其getAll方法获取当前数据, 将数据写入文件队列, 清除当前counter数据
                    for (IAgentCounter counter : counters.values()) {
                        try {
                            if (!counter.isEmpty()) {
                                for (Object e : counter.buildAndGetThriftEntities(INTERVAL, appName, ipAddress, dispatchProcessor.getPort())) {
                                    if (null != e) {
                                        dispatchProcessor.pushMessage(e);
                                    }
                                }
                            }
                        } catch (RuntimeException e) {
                            log.error("RuntimeException occured when dispatching!", e);
                        } catch (Throwable e) {
                            log.error("Throwable(unrecoverable) exception occured when dispatching!", e);
                            //不抛出，否则一个counter出错会导致所有的counter无法上报
                            //throw e;
                        }
                    }
                }
            }
        }, 0, INTERVAL * 1000);
    }


    /**
     * plugin需要向counterRepository注册自己的使用信息, 用于数据统计发送
     *
     * @param pluginName 注册名字, 不可重复, 最好是使用plugin名称, 禁止中文!
     * @param counter    plugin自身的counter实现
     */
    public void register(String pluginName, IAgentCounter counter) throws Exception {
        // 先检测是否已注册过同名缓冲器, 注册过则抛出异常
        if (counters.containsKey(pluginName)) {
            throw new Exception(String.format("counter-%s already registered!", pluginName));
        } else {
            counters.put(pluginName, counter);
            log.debug(String.format("counter-%s Register Success!", pluginName));
        }
    }
}

