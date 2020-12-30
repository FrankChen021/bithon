package com.sbss.bithon.agent.plugin.springweb;

import com.keruyun.commons.agent.collector.entity.SpringRestfulUriPatternEntity;
import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import com.sbss.bithon.agent.dispatcher.metrics.DispatchProcessor;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description : spring mvc method matching enhance handler <br>
 * Date: 18/3/1
 *
 * @author 马至远
 */
public class MethodMatchingHandler extends AbstractMethodIntercepted {
    private static final Logger log = LoggerFactory.getLogger(MethodMatchingHandler.class);

    private static final String CHECK_PERIOD = "checkPeriod";

    private DispatchProcessor dispatchProcessor;

    private Set<String> uriPatterns;

    @Override
    public boolean init() throws Exception {
        int checkPeriod = 10;

        dispatchProcessor = DispatchProcessor.getInstance();

        uriPatterns = ConcurrentHashMap.newKeySet();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                dispatch();
            }
        }, checkPeriod * 1000, checkPeriod * 1000);

        return true;
    }

    @Override
    protected void after(AfterJoinPoint joinPoint) {
        List<String> fuzzyPatterns = new ArrayList<>();
        // 获取注册controller pattern, 检测是否带有路径变量
        Object arg0 = joinPoint.getArgs()[0];
        if (arg0 instanceof RequestMappingInfo) {
            RequestMappingInfo mappingInfo = (RequestMappingInfo) arg0;
            Set<String> patterns = mappingInfo.getPatternsCondition().getPatterns();
            fuzzyPatterns = getFuzzyPatterns(patterns);
        } else {
            log.warn("spring mvc registering mapping pattern with unrecognized class");
        }

        if (null != fuzzyPatterns && !fuzzyPatterns.isEmpty()) {
            uriPatterns.addAll(fuzzyPatterns);
        }
    }

    private List<String> getFuzzyPatterns(Set<String> patterns) {
        List<String> parsedPatterns = new ArrayList<>();
        for (String pattern : patterns) {
            String parsedPattern = pattern.replaceAll("/[^/]*\\{[^}]*}[^/]*", "/*");
            if (parsedPattern.contains("*")) {
                parsedPatterns.add(parsedPattern);
            }
        }

        return parsedPatterns;
    }

    private SpringRestfulUriPatternEntity buildEntity() {
        // 组装数据
        List<String> toSendPatterns = new ArrayList<>(uriPatterns);
        uriPatterns.clear();

        return new SpringRestfulUriPatternEntity(dispatchProcessor.getAppName(),
                                                 dispatchProcessor.getIpAddress(),
                                                 dispatchProcessor.getPort(),
                                                 System.currentTimeMillis(),
                                                 toSendPatterns);
    }

    private void dispatch() {
        try {
            if (dispatchProcessor.ready && null != uriPatterns && !uriPatterns.isEmpty()) {
                dispatchProcessor.pushMessage(buildEntity());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
