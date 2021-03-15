package com.sbss.bithon.agent.plugin.jdk.httpclient;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.IBithonObject;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/14 11:13 下午
 */
public class HttpsClientNewInterceptor extends AbstractInterceptor {
    /**
     * inject HttpURLConnection instance, which creates HttpClient instance, into the instance of HttpClient as its parent
     *
     * @param aopContext
     */
    @Override
    public void onMethodLeave(AopContext aopContext) {
        IBithonObject injectedObject = aopContext.castReturningAs();
        injectedObject.setInjectedObject(aopContext.getArgs()[6]);
    }
}
