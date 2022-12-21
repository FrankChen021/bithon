package org.bithon.agent.plugin.apache.ozone.interceptor;/*
 *    Copyright 2020 bithon.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

import org.apache.hadoop.hdds.scm.XceiverClientRatis;
import org.apache.ratis.client.RaftClient;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.component.commons.utils.ReflectionUtils;

import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link XceiverClientRatis#connect()}
 *
 * @author Frank Chen
 * @date 21/12/22 5:13 pm
 */
public class XceiverClientRatis$Connect extends AbstractInterceptor {
    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) throws Exception {
        AtomicReference<RaftClient> client = (AtomicReference<RaftClient>) ReflectionUtils.getFieldValue(aopContext.getTarget(), "client");

        if (client != null && client.get() == null) {
            XceiverClientRatis ratisClient = aopContext.castTargetAs();

            // save the node info
            ((IBithonObject) aopContext.getTarget()).setInjectedObject(ratisClient.getPipeline().getLeaderNode());
        }

        return InterceptionDecision.SKIP_LEAVE;
    }
}
