package com.sbss.bithon.agent.plugin.httpcomponent;

import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import com.sbss.bithon.agent.core.interceptor.BeforeJoinPoint;

import java.util.HashSet;

/**
 * Description : apache http component(client) handler <br>
 * Date: 17/10/27
 *
 * @author 马至远
 */
public class HttpComponentHandler extends AbstractMethodIntercepted {
    @Override
    public boolean init() throws Exception {
        HttpCounter.getInstance().setIgnoredSuffixes(new HashSet<>());
        return true;
    }

    @Override
    protected Object createContext(BeforeJoinPoint joinPoint) {
        return (Long) System.nanoTime();
    }

    @Override
    protected void after(AfterJoinPoint joinPoint) {
        HttpCounter.getInstance().add(joinPoint);
    }
}
