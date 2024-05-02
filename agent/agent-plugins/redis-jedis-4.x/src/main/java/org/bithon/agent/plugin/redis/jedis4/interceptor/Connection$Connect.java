/*
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

package org.bithon.agent.plugin.redis.jedis4.interceptor;

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;
import org.bithon.component.commons.utils.ReflectionUtils;
import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisSocketFactory;
import redis.clients.jedis.JedisSocketFactory;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * @author frank.chen021@outlook.com
 * @date 2/5/24 3:50 pm
 */
public class Connection$Connect extends AroundInterceptor {

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        Connection conn = aopContext.getTargetAs();
        if (conn.isConnected()) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        ITraceSpan span = TraceContextFactory.newSpan("jedis");
        if (span == null) {
            return InterceptionDecision.CONTINUE;
        }

        aopContext.setSpan(span.method(aopContext.getTargetClass().getName(), aopContext.getMethod())
                               .kind(SpanKind.CLIENT)
                               .tag(Tags.Database.SYSTEM, "redis")
                               .start());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        String hostAndPort = getRemoteAddress(aopContext.getTargetAs());

        // Update the address
        ((IBithonObject) aopContext.getTargetAs()).setInjectedObject(hostAndPort);

        ITraceSpan span = aopContext.getSpan();
        if (span != null) {
            span.tag(aopContext.getException())
                .tag(Tags.Net.PEER, hostAndPort)
                .finish();
        }
    }

    private String getRemoteAddress(Connection connection) {
        JedisSocketFactory socketFactory = (JedisSocketFactory) ReflectionUtils.getFieldValue(connection, "socketFactory");
        if (socketFactory instanceof DefaultJedisSocketFactory) {
            return ((DefaultJedisSocketFactory) socketFactory).getHostAndPort().toString();
        }

        Socket socket = (Socket) ReflectionUtils.getFieldValue(connection, "socket");
        if (socket == null) {
            return null;
        }
        SocketAddress socketAddress = socket.getRemoteSocketAddress();
        if (socketAddress instanceof InetSocketAddress) {
            return ((InetSocketAddress) socketAddress).getHostString() + ":" + ((InetSocketAddress) socketAddress).getPort();
        }

        return socketAddress.toString();
    }
}
