package com.sbss.bithon.agent.plugin.lettuce;

import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.plugin.descriptor.MethodPointCutDescriptorBuilder;
import com.sbss.bithon.agent.plugin.lettuce.interceptor.RedisClientConnect;
import shaded.net.bytebuddy.description.method.MethodDescription;
import shaded.net.bytebuddy.matcher.ElementMatcher;
import shaded.net.bytebuddy.matcher.ElementMatchers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 * @date 2021-02-27 16:58:01
 */
public class LettucePlugin extends AbstractPlugin {

    private ElementMatcher.Junction<MethodDescription> getExclusion() {
        Set<String> excludeMethodNames = new HashSet<>();
        excludeMethodNames.addAll(Arrays.asList("clone", "equals", "finalize", "getClass", "hashCode", "notify", "notifyAll", "toString", "wait"));
        excludeMethodNames.addAll(Arrays.asList("dispatch", "getConnection", "setAutoFlushCommands", "setTimeout", "createMono", "createDissolvingFlux"));
        return ElementMatchers.namedOneOf(excludeMethodNames.toArray(new String[0]));
    }

    /**
     * Since no Connection objects are intercepted,
     * we have to instrument Connection objects to forward Endpoint info from RedisClient to Command objects
     * This makes {@link RedisClientConnect} work
     */
    @Override
    public String[] getClassInstrumentations() {
        return new String[]{
            "io.lettuce.core.StatefulRedisConnectionImpl",
            "io.lettuce.core.pubsub.StatefulRedisPubSubConnectionImpl",
            "io.lettuce.core.pubsub.StatefulRedisClusterPubSubConnectionImpl",
            "io.lettuce.core.masterslave.StatefulRedisMasterSlaveConnectionImpl",
            "io.lettuce.core.sentinel.StatefulRedisSentinelConnectionImpl"
        };
    }

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Arrays.asList(
            //
            // add interceptor for forward Endpoint to Connection
            //
            forClass("io.lettuce.core.RedisClient")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onAllMethods("connect")
                        .to("com.sbss.bithon.agent.plugin.lettuce.interceptor.RedisClientConnect"),

                    MethodPointCutDescriptorBuilder.build()
                        .onAllMethods("connectAsync")
                        .to("com.sbss.bithon.agent.plugin.lettuce.interceptor.RedisClientConnect"),

                    MethodPointCutDescriptorBuilder.build()
                        .onAllMethods("connectPubSub")
                        .to("com.sbss.bithon.agent.plugin.lettuce.interceptor.RedisClientConnect"),

                    MethodPointCutDescriptorBuilder.build()
                        .onAllMethods("connectPubSubAsync")
                        .to("com.sbss.bithon.agent.plugin.lettuce.interceptor.RedisClientConnect"),

                    MethodPointCutDescriptorBuilder.build()
                        .onAllMethods("connectSentinel")
                        .to("com.sbss.bithon.agent.plugin.lettuce.interceptor.RedisClientConnect"),

                    MethodPointCutDescriptorBuilder.build()
                        .onAllMethods("connectSentinelAsync")
                        .to("com.sbss.bithon.agent.plugin.lettuce.interceptor.RedisClientConnect")
                ),

            forClass("io.lettuce.core.DefaultConnectionFuture")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onAllMethods("get")
                        .to("com.sbss.bithon.agent.plugin.lettuce.interceptor.DefaultConnectionFutureGet"),

                    MethodPointCutDescriptorBuilder.build()
                        .onAllMethods("join")
                        .to("com.sbss.bithon.agent.plugin.lettuce.interceptor.DefaultConnectionFutureGet")
                ),

            //
            // issue command
            //
            forClass("io.lettuce.core.cluster.RedisAdvancedClusterAsyncCommandsImpl")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        //.onMethod(exclusiveMatcher.and(ElementMatchers.returns(ElementMatchers.nameStartsWith("io.lettuce.core.protocol.RedisFuture"))))
                        .onMethodAndArgs("dispatch", "io.lettuce.core.protocol.RedisCommand<K, V, T>")
                        .to("com.sbss.bithon.agent.plugin.lettuce.interceptor.RedisAsyncCommandDispatch")
                ),

            forClass("io.lettuce.core.RedisAsyncCommandsImpl")
                .debug()
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        //.onMethod(exclusiveMatcher.and(ElementMatchers.returns(ElementMatchers.nameStartsWith("io.lettuce.core.RedisFuture"))).and(isPublic())
                        .onMethodAndArgs("dispatch", "io.lettuce.core.protocol.RedisCommand<K, V, T>")
                        .to("com.sbss.bithon.agent.plugin.lettuce.interceptor.RedisAsyncCommandDispatch")
                ),

            forClass("io.lettuce.core.cluster.RedisClusterPubSubAsyncCommandsImpl")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        //.onMethod(exclusiveMatcher)
                        .onMethodAndArgs("dispatch", "io.lettuce.core.protocol.RedisCommand<K, V, T>")
                        .to("com.sbss.bithon.agent.plugin.lettuce.interceptor.RedisAsyncCommandDispatch")
                ),

            forClass("io.lettuce.core.pubsub.RedisPubSubAsyncCommandsImpl")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        //.onMethod(exclusiveMatcher)
                        .onMethodAndArgs("dispatch", "io.lettuce.core.protocol.RedisCommand<K, V, T>")
                        .to("com.sbss.bithon.agent.plugin.lettuce.interceptor.RedisAsyncCommandDispatch")
                ),

            //
            // request sync complete
            //
            forClass("io.lettuce.core.protocol.AsyncCommand")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onMethodAndNoArgs("completeResult")
                        .to("com.sbss.bithon.agent.plugin.lettuce.interceptor.RedisAsyncCommandComplete"),

                    MethodPointCutDescriptorBuilder.build()
                        .onMethodAndArgs("cancel", "java.lang.boolean")
                        .to("com.sbss.bithon.agent.plugin.lettuce.interceptor.RedisAsyncCommandComplete")
                )
        );
    }
}
