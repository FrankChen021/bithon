/*
 *    Copyright 2020 bithon.cn
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.sbss.bithon.agent.plugin.thread;

import com.sbss.bithon.agent.core.dispatcher.Dispatcher;
import com.sbss.bithon.agent.core.dispatcher.Dispatchers;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


/**
 * @author frankchen
 */
public class ThreadDumpService {
    private static final Logger log = LoggerFactory.getLogger(ThreadDumpService.class);
    private static final String THREAD_NAME = "threadName";
    private static final String THREAD_ID = "threadId";
    private static final String THREAD_IS_DAEMON = "threadIsDaemon";
    private static final String THREAD_PRIORITY = "threadPriority";
    private static final String THREAD_STATE = "threadState";
    private static final String THREAD_DECLARER = "threadDeclarer";
    private static final String THREAD_CPU_TIME_MILLIS = "threadCpuTimeMillis";
    private static final String THREAD_USER_TIME_MILLIS = "threadUserTimeMillis";
    private static final long THREAD_TIME_UNAVAILABLE = -1;
    private static final String THREAD_ENTITY_NAME = "jvm-thread";
    private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();
    private Dispatcher dispatcher;
    private int checkPeriod;

    public boolean initialize() {
        checkPeriod = 10;

        dispatcher = Dispatchers.getOrCreate(Dispatchers.DISPATCHER_NAME_METRIC);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Map<Thread, StackTraceElement[]> stackTraces = Thread.getAllStackTraces();
                List<Thread> threads = new ArrayList<>(stackTraces.keySet());

                for (Thread thread : threads) {
                    dispatch(thread, stackTraces.get(thread));
                }
            }
        }, 0, checkPeriod * 1000L);

        return false;
    }
/*
    private ExtendEntity buildEntity(Thread thread,
                                     StackTraceElement[] stackTraces) {
        Map<String, String> threadStringFields = new HashMap<>();
        Map<String, Long> threadNumberFields = new HashMap<>();
        boolean cpuTimeEnabled = THREAD_MX_BEAN.isThreadCpuTimeSupported() && THREAD_MX_BEAN.isThreadCpuTimeEnabled();

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

        return new ExtendEntity(dispatcher.getAppName(),
                                dispatcher.getIpAddress(),
                                dispatcher.getPort(),
                                System.currentTimeMillis(),
                                checkPeriod,
                                1,
                                THREAD_ENTITY_NAME,
                                threadStringFields,
                                threadNumberFields);
    }*/

    private void dispatch(Thread thread,
                          StackTraceElement[] stackTraces) {
        try {
            if (dispatcher.isReady()) {
                /*dispatcher.sendMetrics(buildEntity(thread, stackTraces));*/
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
