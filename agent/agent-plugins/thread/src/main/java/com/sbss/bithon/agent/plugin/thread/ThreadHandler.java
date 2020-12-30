package com.sbss.bithon.agent.plugin.thread;

import com.keruyun.commons.agent.collector.entity.ExtendEntity;
import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import com.sbss.bithon.agent.dispatcher.metrics.DispatchProcessor;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.*;

/**
 * Description : <br>
 * Date: 17/9/12
 *
 * @author 马至远
 */
public class ThreadHandler extends AbstractMethodIntercepted {
    private static final Logger log = LoggerFactory.getLogger(ThreadHandler.class);

    /**
     * name
     */
    private static final String THREAD_NAME = "threadName";
    /**
     * id
     */
    private static final String THREAD_ID = "threadId";
    /**
     * 是否守护线程
     */
    private static final String THREAD_IS_DAEMON = "threadIsDaemon";
    /**
     * 优先级
     */
    private static final String THREAD_PRIORITY = "threadPriority";
    /**
     * 状态
     */
    private static final String THREAD_STATE = "threadState";
    /**
     * 调用者信息
     */
    private static final String THREAD_DECLARER = "threadDeclarer";
    /**
     * cpu使用时间
     */
    private static final String THREAD_CPU_TIME_MILLIS = "threadCpuTimeMillis";
    /**
     * 用户时间
     */
    private static final String THREAD_USER_TIME_MILLIS = "threadUserTimeMillis";

    private static final long THREAD_TIME_UNAVAILABLE = -1;
    private static final String THREAD_ENTITY_NAME = "jvm-thread";

    private DispatchProcessor dispatchProcessor;

    private int checkPeriod;

    private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();

    @Override
    public boolean init() {
        checkPeriod = 10;

        dispatchProcessor = DispatchProcessor.getInstance();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                // 获取当前线程信息
                Map<Thread, StackTraceElement[]> stackTraces = Thread.getAllStackTraces();
                List<Thread> threads = new ArrayList<>(stackTraces.keySet());

                for (Thread thread : threads) {
                    dispatch(thread, stackTraces.get(thread));
                }
            }
        }, 0, checkPeriod * 1000L);

        return false;
    }

    /**
     * 创建threadEntity数据
     *
     * @return 线程数据
     */
    private ExtendEntity buildEntity(Thread thread,
                                     StackTraceElement[] stackTraces) {
        Map<String, String> threadStringFields = new HashMap<>();
        Map<String, Long> threadNumberFields = new HashMap<>();
        boolean cpuTimeEnabled = THREAD_MX_BEAN.isThreadCpuTimeSupported() && THREAD_MX_BEAN.isThreadCpuTimeEnabled();

        // 获取当前thread的数据并拼装至entity
        threadStringFields.put(THREAD_NAME, thread.getName());
        threadNumberFields.put(THREAD_ID, thread.getId());
        threadStringFields.put(THREAD_IS_DAEMON, thread.isDaemon() ? "yes" : "no");
        threadNumberFields.put(THREAD_PRIORITY, (long) thread.getPriority());
        threadStringFields.put(THREAD_STATE, thread.getState().toString());
        threadStringFields.put(THREAD_DECLARER, stackTraces.length > 0 ? stackTraces[0].toString() : null);
        threadNumberFields.put(THREAD_CPU_TIME_MILLIS,
                               cpuTimeEnabled ? THREAD_MX_BEAN.getThreadCpuTime(thread.getId())
                                   : THREAD_TIME_UNAVAILABLE);
        threadNumberFields.put(THREAD_USER_TIME_MILLIS,
                               cpuTimeEnabled ? THREAD_MX_BEAN.getThreadUserTime(thread.getId())
                                   : THREAD_TIME_UNAVAILABLE);

        return new ExtendEntity(dispatchProcessor.getAppName(),
                                dispatchProcessor.getIpAddress(),
                                dispatchProcessor.getPort(),
                                System.currentTimeMillis(),
                                checkPeriod,
                                1,
                                THREAD_ENTITY_NAME,
                                threadStringFields,
                                threadNumberFields);
    }

    private void dispatch(Thread thread,
                          StackTraceElement[] stackTraces) {
        try {
            if (dispatchProcessor.ready) {
                dispatchProcessor.pushMessage(buildEntity(thread, stackTraces));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
