package com.sbss.bithon.agent.plugin.httpclient.jdk;

import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.boot.aop.IBithonObject;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/21 11:13 下午
 */
public class HttpClientNewInterceptor extends AbstractInterceptor {
    /**
     * inject HttpURLConnection instance, which creates HttpClient instance, into the instance of HttpClient as its parent
     *
     * @param aopContext
     */
    @Override
    public void onMethodLeave(AopContext aopContext) {
        IBithonObject injectedObject = aopContext.castReturningAs();
        injectedObject.setInjectedObject(aopContext.getArgs()[4]);
    }
}
