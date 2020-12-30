package com.sbss.bithon.agent.plugin.undertow;

import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import com.sbss.bithon.agent.core.interceptor.BeforeJoinPoint;
import com.sbss.bithon.agent.core.util.UserAgentFilter;
import com.sbss.bithon.agent.dispatcher.metrics.counter.AgentCounterRepository;
import com.sbss.bithon.agent.dispatcher.metrics.counter.IAgentCounter;
import io.undertow.server.HttpServerExchange;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class ServerExchangeHandler extends AbstractMethodIntercepted {
    private Set<String> ignoredSuffixes;

    private static final String UNDERTOW_SERVER_EXCHANGE_BUFFER_MANAGER_NAME = "undertow-server-exchange";

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

        // 向counterRepository注册
        requestCounter = new RequestCounter();
        counterRepository.register(UNDERTOW_SERVER_EXCHANGE_BUFFER_MANAGER_NAME, requestCounter);

        return true;
    }

    @Override
    protected void before(BeforeJoinPoint joinPoint) {
        HttpServerExchange exchange = (HttpServerExchange) joinPoint.getTarget();

        boolean needIgnore = needIgnore(exchange);
        if (!needIgnore) {
            long beginTime = System.nanoTime();
            joinPoint.setArgs(new Object[]{beginTime});
            exchange.addExchangeCompleteListener((listener,
                                                  next) -> {
                requestCounter.add(joinPoint);
                next.proceed();
            });
        }
    }

    private boolean needIgnore(HttpServerExchange exchange) {
        if (UserAgentFilter.isFiltered(exchange.getRequestHeaders().getFirst("User-Agent"))) {
            return true;
        }

        String uri = exchange.getRequestPath();
        String suffix = uri.substring(uri.lastIndexOf(".") + 1).toLowerCase();
        return ignoredSuffixes.contains(suffix);
    }
}
