package com.sbss.bithon.agent.plugin.thread;

import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;

/**
 * Description : Runnable or Callable handler on construct, for trace <br>
 * Date: 18/6/15
 *
 * @author 马至远
 */
public class RunnableOrCallableConstructHandler extends AbstractMethodIntercepted {
    @Override
    public boolean init() throws Exception {
        return false;
    }

    @Override
    protected void onConstruct(Object constructedObject,
                               Object[] args) throws Exception {

        super.onConstruct(constructedObject, args);
    }
}
