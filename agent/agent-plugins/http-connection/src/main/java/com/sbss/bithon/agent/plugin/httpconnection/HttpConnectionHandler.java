package com.sbss.bithon.agent.plugin.httpconnection;

import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import com.sbss.bithon.agent.core.interceptor.BeforeJoinPoint;
import com.sbss.bithon.agent.dispatcher.metrics.counter.AgentCounterRepository;
import com.sbss.bithon.agent.dispatcher.metrics.counter.IAgentCounter;

import java.net.HttpURLConnection;

/**
 * Description : jdk http-connection handler <br>
 * Date: 17/10/25
 *
 * @author 马至远
 */
public class HttpConnectionHandler extends AbstractMethodIntercepted {
    private IAgentCounter counter;

    @Override
    public boolean init() throws Exception {
        // 获取CounterRepository 实例
        AgentCounterRepository counterRepository = AgentCounterRepository.getInstance();

        // 向counterRepository注册, 开始统计request信息
        counter = new HttpCounter();
        counterRepository.register(HttpCounter.COUNTER_NAME, counter);

        return true;
    }

    @Override
    protected Object createContext(BeforeJoinPoint joinPoint) {
        return System.nanoTime();
    }

    @Override
    protected void after(AfterJoinPoint joinPoint) {
        HttpURLConnection httpUrlConnection = (HttpURLConnection) joinPoint.getTarget();

//        try {
//            //  只拦截inputstream == null 的请求, 其他请求都是内部实现中冗余的
//            if (null == httpUrlConnection.getInputStream()) {
//                counter.add(joinPoint);
//            }
//        } catch (IOException e) {
//            log.error("httpUrlConnection: get input stream failed " + e);
//        }
    }
}
