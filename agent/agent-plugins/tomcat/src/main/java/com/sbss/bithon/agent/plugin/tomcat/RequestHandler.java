package com.sbss.bithon.agent.plugin.tomcat;

import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import com.sbss.bithon.agent.core.interceptor.BeforeJoinPoint;
import com.sbss.bithon.agent.core.util.UserAgentFilter;
import com.sbss.bithon.agent.dispatcher.metrics.counter.AgentCounterRepository;
import com.sbss.bithon.agent.dispatcher.metrics.counter.IAgentCounter;
import org.apache.coyote.Request;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class RequestHandler extends AbstractMethodIntercepted {
    private static final String KEY_IGNORED_SUFFIXES = "ignoredSuffixes";
    private static final String TOMCAT_REQUEST_BUFFER_MANAGER_NAME = "tomcat-request";

    private Set<String> ignoredSuffixes;

    /**
     * 数据记录器, 用于统计数据
     */
    private IAgentCounter requestCounter;

    @Override
    public boolean init() throws Exception {
        ignoredSuffixes = Arrays.stream("html, js, css, jpg, gif, png, swf, ttf, ico, woff, woff2, json, eot, svg".split(","))
            .map(x -> x.trim().toLowerCase())
            .collect(Collectors.toSet());
        // 获取CounterRepository 实例
        AgentCounterRepository counterRepository = AgentCounterRepository.getInstance();

        // 向counterRepository注册, 开始统计request信息
        requestCounter = new RequestCounter();
        counterRepository.register(TOMCAT_REQUEST_BUFFER_MANAGER_NAME, requestCounter);

        return true;
    }

    @Override
    protected Object createContext(BeforeJoinPoint joinPoint) {
        return System.nanoTime();
    }

    @Override
    protected void after(AfterJoinPoint joinPoint) {
        if (!ignoreRequest(joinPoint)) {
            requestCounter.add(joinPoint);
        }
    }

    private boolean ignoreRequest(AfterJoinPoint joinPoint) {
        Request request = (Request) joinPoint.getArgs()[0];
        if (UserAgentFilter.isFiltered(request.getHeader("User-Agent"))) {
            return true;
        }

        String uri = request.requestURI().toString();
        if (uri != null) {
            int index = uri.lastIndexOf(".") + 1;
            if (uri.length() > index) {
                String suffix = uri.substring(index).toLowerCase();
                return ignoredSuffixes.contains(suffix);
            }
        }
        return false;
    }
}
