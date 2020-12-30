package com.sbss.bithon.agent.plugin.okhttp3;

import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import com.sbss.bithon.agent.core.interceptor.BeforeJoinPoint;
import com.sbss.bithon.agent.dispatcher.metrics.counter.AgentCounterRepository;
import com.sbss.bithon.agent.dispatcher.metrics.counter.IAgentCounter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Description : OkHttp3 agent plugin <br>
 * Date: 17/10/23
 *
 * @author 马至远
 */
public class OkHttp3Handler extends AbstractMethodIntercepted {
    private IAgentCounter counter;

    @Override
    public boolean init() throws Exception {

        Set<String> ignoredSuffixes = Arrays.stream("html, js, css, jpg, gif, png, swf, ttf, ico, woff, woff2, json, eot, svg".split(","))
            .map(x -> x.trim().toLowerCase())
            .collect(Collectors.toSet());

        // 获取CounterRepository 实例
        AgentCounterRepository counterRepository = AgentCounterRepository.getInstance();

        // 向counterRepository注册, 开始统计request信息
        counter = new HttpCounter(ignoredSuffixes);
        counterRepository.register(HttpCounter.COUNTER_NAME, counter);

        return true;
    }

    @Override
    protected Object createContext(BeforeJoinPoint joinPoint) {
        return System.nanoTime();
    }

    @Override
    protected void after(AfterJoinPoint joinPoint) {
        counter.add(joinPoint);
    }
}
