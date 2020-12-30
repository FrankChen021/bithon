package com.sbss.bithon.agent.plugin.jetty;

import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import com.sbss.bithon.agent.core.interceptor.BeforeJoinPoint;
import com.sbss.bithon.agent.core.util.UserAgentFilter;
import com.sbss.bithon.agent.dispatcher.metrics.counter.AgentCounterRepository;
import com.sbss.bithon.agent.dispatcher.metrics.counter.IAgentCounter;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class RequestHandler extends AbstractMethodIntercepted {
    private static final String JETTY_REQUEST_BUFFER_MANAGER_NAME = "jetty-request";

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
        counterRepository.register(JETTY_REQUEST_BUFFER_MANAGER_NAME, requestCounter);

        return true;
    }

    @Override
    protected Object createContext(BeforeJoinPoint joinPoint) {
        HttpServletRequest request = (HttpServletRequest) joinPoint.getArgs()[2];
        return isFiltered(request) ? null : System.nanoTime();
    }

    @Override
    protected void after(AfterJoinPoint joinPoint) {
        if (joinPoint.getContext() != null) {
            requestCounter.add(joinPoint);
        }
    }

    private boolean isFiltered(HttpServletRequest request) {
        if (UserAgentFilter.isFiltered(request.getHeader("User-Agent"))) {
            return true;
        }

        String uri = request.getRequestURI();
        String suffix = uri.substring(uri.lastIndexOf(".") + 1).toLowerCase();
        return ignoredSuffixes.contains(suffix);
    }
}
