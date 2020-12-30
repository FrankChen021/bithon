package com.sbss.bithon.agent.plugin.jedis;

import com.sbss.bithon.agent.core.transformer.AbstractClassTransformer;
import com.sbss.bithon.agent.core.transformer.matcher.DefaultMethodNameMatcher;
import com.sbss.bithon.agent.core.transformer.plugin.AgentHandler;
import com.sbss.bithon.agent.core.transformer.plugin.IAgentHandler;
import com.sbss.bithon.agent.core.transformer.plugin.MethodPointCut;

/**
 * Description : Jedis config location <br>
 * Date: 17/11/1
 *
 * @author 马至远
 */
public class JedisTransformer extends AbstractClassTransformer {

    @Override
    public IAgentHandler[] getHandlers() {
        return new IAgentHandler[]{
            new AgentHandler("com.sbss.bithon.agent.plugin.jedis.JedisRequestHandler",
                             //2.9.x
                             new MethodPointCut("redis.clients.jedis.Client",
                                                DefaultMethodNameMatcher.byNameAndEmptyArgs("readProtocolWithCheckingBroken")),
                             new MethodPointCut("redis.clients.jedis.Client",
                                                DefaultMethodNameMatcher.byNameAndArgs("sendCommand",
                                                                                       "redis.clients.jedis.Protocol$Command",
                                                                                       "[[B")),
                             //3.x
                             new MethodPointCut("redis.clients.jedis.Client",
                                                DefaultMethodNameMatcher.byNameAndArgs("sendCommand",
                                                                                       "redis.clients.jedis.commands.ProtocolCommand",
                                                                                       "[[B"))

            ),
            new AgentHandler("com.sbss.bithon.agent.plugin.jedis.JedisOutputStreamHandler",
                             new MethodPointCut("redis.clients.util.RedisOutputStream",
                                                DefaultMethodNameMatcher.byNameAndEmptyArgs("flushBuffer")),
                             new MethodPointCut("redis.clients.jedis.util.RedisOutputStream",
                                                DefaultMethodNameMatcher.byNameAndEmptyArgs("flushBuffer"))),

            new AgentHandler("com.sbss.bithon.agent.plugin.jedis.JedisInputStreamHandler",
                             //2.9.x
                             new MethodPointCut("redis.clients.util.RedisInputStream",
                                                DefaultMethodNameMatcher.byNameAndEmptyArgs("ensureFill")),
                             //3.x
                             new MethodPointCut("redis.clients.jedis.util.RedisInputStream",
                                                DefaultMethodNameMatcher.byNameAndEmptyArgs("ensureFill"))),

            new AgentHandler("com.sbss.bithon.agent.plugin.jedis.JedisConnectionHandler",
                             new MethodPointCut("redis.clients.jedis.Connection",
                                                DefaultMethodNameMatcher.byNameAndEmptyArgs("connect"))),};
    }
}
