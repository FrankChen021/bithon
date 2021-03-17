package com.sbss.bithon.agent.plugin.jedis;

import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.plugin.descriptor.MethodPointCutDescriptorBuilder;

import java.util.Arrays;
import java.util.List;

import static com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class JedisPlugin extends AbstractPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(

            forClass("redis.clients.jedis.Connection")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onMethodAndNoArgs("connect")
                        .to("com.sbss.bithon.agent.plugin.jedis.interceptor.JedisConnectionConnect")
                ),

            //2.9.x
            forClass("redis.clients.jedis.Client")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onMethodAndNoArgs("readProtocolWithCheckingBroken")
                        .to("com.sbss.bithon.agent.plugin.jedis.interceptor.JedisClientReadProtocol"),

                    MethodPointCutDescriptorBuilder.build()
                        .onMethodAndArgs("sendCommand",
                                         "redis.clients.jedis.Protocol$Command", "[[B")
                        .to("com.sbss.bithon.agent.plugin.jedis.interceptor.JedisClientSendCommand"),

                    //3.x
                    MethodPointCutDescriptorBuilder.build()
                        .onMethodAndArgs("sendCommand",
                                         "redis.clients.jedis.commands.ProtocolCommand", "[[B")
                        .to("com.sbss.bithon.agent.plugin.jedis.interceptor.JedisClientSendCommand")
                ),

            forClass("redis.clients.util.RedisOutputStream")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onMethodAndNoArgs("flushBuffer")
                        .to("com.sbss.bithon.agent.plugin.jedis.interceptor.JedisOutputStreamFlushBuffer")
                ),

            forClass("redis.clients.jedis.util.RedisOutputStream")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onMethodAndNoArgs("flushBuffer")
                        .to("com.sbss.bithon.agent.plugin.jedis.interceptor.JedisOutputStreamFlushBuffer")
                ),

            //2.9.x
            forClass("redis.clients.util.RedisInputStream")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onMethodAndNoArgs("ensureFill")
                        .to("com.sbss.bithon.agent.plugin.jedis.interceptor.JedisInputStreamEnsureFill")
                ),

            //3.x
            forClass("redis.clients.jedis.util.RedisInputStream")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onMethodAndNoArgs("ensureFill")
                        .to("com.sbss.bithon.agent.plugin.jedis.interceptor.JedisInputStreamEnsureFill")
                )
        );
    }
}
