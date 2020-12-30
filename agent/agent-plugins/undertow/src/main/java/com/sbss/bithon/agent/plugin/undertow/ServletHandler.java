package com.sbss.bithon.agent.plugin.undertow;

import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import com.sbss.bithon.agent.core.interceptor.BeforeJoinPoint;

/**
 * Description : <br>
 * Date: 18/6/14
 *
 * @author 马至远
 */
public class ServletHandler extends AbstractMethodIntercepted {
    @Override
    public boolean init() throws Exception {
        return true;
    }

    @Override
    protected void before(BeforeJoinPoint joinPoint) {

    }
}
