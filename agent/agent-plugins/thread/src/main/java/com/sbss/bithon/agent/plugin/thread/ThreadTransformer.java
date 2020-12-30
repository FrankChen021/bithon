package com.sbss.bithon.agent.plugin.thread;

import com.sbss.bithon.agent.core.transformer.AbstractClassTransformer;
import com.sbss.bithon.agent.core.transformer.plugin.IAgentHandler;

/**
 * Description : thread transformer <br>
 * Date: 17/9/12
 *
 * @author 马至远
 */
public class ThreadTransformer extends AbstractClassTransformer {
    @Override
    public IAgentHandler[] getHandlers() {
//        return new AgentHandler[]{
//                new AgentHandler() {
//                    @Override
//                    public String getHandlerClass() {
//                        return "com.keruyun.commons.agent.plugin.thread.ThreadHandler";
//                    }
//
//                    @Override
//                    public Map<String, String> getProperties() {
//                        Map<String, String> properties = new HashMap<>();
//                        properties.put("checkPeriod", "10");
//                        return properties;
//                    }
//
//                    @Override
//                    public AgentPointcut[] getPointcuts() {
//                        return new AgentPointcut[]{};
//                    }
//                }
//        };

//        // TODO 暂时屏蔽thread监控插件, 等做完了后端存储, 再开放这个功能
//        return new AgentHandler[]{
//                new AgentHandler() {
//                    @Override
//                    public String getHandlerClass() {
//                        return "com.keruyun.commons.agent.plugin.thread.ThreadConstructHandler";
//                    }
//
//                    @Override
//                    public Map<String, String> getProperties() {
//                        return null;
//                    }
//
//                    @Override
//                    public AgentPointcut[] getPointcuts() {
//                        return new AgentPointcut[] {
//                                new AgentPointcut() {
//                                    @Override
//                                    public AgentClassMatcher getClassMatcher() {
//                                        return DefaultClassNameMatcher.byName("java.lang.Thread");
//                                    }
//
//                                    @Override
//                                    public AgentMethodMatcher getMethodMatcher() {
//                                        return DefaultMethodNameMatcher.byAllConstructors();
//                                    }
//                                }
//                        };
//                    }
//                }
//        };
        return new IAgentHandler[0];
    }
}
