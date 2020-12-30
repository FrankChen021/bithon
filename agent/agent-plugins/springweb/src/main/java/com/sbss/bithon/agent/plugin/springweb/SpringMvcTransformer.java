package com.sbss.bithon.agent.plugin.springweb;

import com.sbss.bithon.agent.core.transformer.AbstractClassTransformer;
import com.sbss.bithon.agent.core.transformer.matcher.AgentClassMatcher;
import com.sbss.bithon.agent.core.transformer.matcher.AgentMethodMatcher;
import com.sbss.bithon.agent.core.transformer.matcher.DefaultClassNameMatcher;
import com.sbss.bithon.agent.core.transformer.matcher.DefaultMethodNameMatcher;
import com.sbss.bithon.agent.core.transformer.plugin.IAgentHandler;
import com.sbss.bithon.agent.core.transformer.plugin.IMethodPointCut;

/**
 * Description : spring mvc transformer <br>
 * Date: 18/3/1
 *
 * @author 马至远
 */
public class SpringMvcTransformer extends AbstractClassTransformer {
    @Override
    public IAgentHandler[] getHandlers() {
        return new IAgentHandler[]{new IAgentHandler() {
            @Override
            public String getHandlerClass() {
                return "com.keruyun.commons.agent.plugin.springmvc.MethodMatchingHandler";
            }

            @Override
            public IMethodPointCut[] getPointcuts() {
                return new IMethodPointCut[]{new IMethodPointCut() {
                    @Override
                    public AgentClassMatcher getClassMatcher() {
                        return DefaultClassNameMatcher.byName("org.springframework.web.servlet.handler.AbstractHandlerMethodMapping$MappingRegistry");
                    }

                    @Override
                    public AgentMethodMatcher getMethodMatcher() {
                        return DefaultMethodNameMatcher.byNameAndArgs("register",
                                                                      new String[]{"T", "java.lang.Object",
                                                                          "java.lang.reflect.Method"});
                    }
                }};
            }
        }, new IAgentHandler() {
            @Override
            public String getHandlerClass() {
                return "com.keruyun.commons.agent.plugin.springmvc.RestTemplateHandler";
            }

            @Override
            public IMethodPointCut[] getPointcuts() {
                return new IMethodPointCut[]{new IMethodPointCut() {
                    @Override
                    public AgentClassMatcher getClassMatcher() {
                        return DefaultClassNameMatcher.byName("org.springframework.web.client.RestTemplate");
                    }

                    @Override
                    public AgentMethodMatcher getMethodMatcher() {
                        return DefaultMethodNameMatcher.byNameAndArgs("execute",
                                                                      new String[]{"java.net.URI",
                                                                          "org.springframework.http.HttpMethod",
                                                                          "org.springframework.web.client.RequestCallback",
                                                                          "org.springframework.web.client.ResponseExtractor<T>"});
                    }
                }, new IMethodPointCut() {
                    @Override
                    public AgentClassMatcher getClassMatcher() {
                        return DefaultClassNameMatcher.byName("org.springframework.web.client.RestTemplate");
                    }

                    @Override
                    public AgentMethodMatcher getMethodMatcher() {
                        return DefaultMethodNameMatcher.byNameAndArgs("execute",
                                                                      new String[]{"java.lang.String",
                                                                          "org.springframework.http.HttpMethod",
                                                                          "org.springframework.web.client.RequestCallback",
                                                                          "org.springframework.web.client.ResponseExtractor<T>",
                                                                          "[Ljava.lang.Object;"});
                    }
                }, new IMethodPointCut() {
                    @Override
                    public AgentClassMatcher getClassMatcher() {
                        return DefaultClassNameMatcher.byName("org.springframework.web.client.RestTemplate");
                    }

                    @Override
                    public AgentMethodMatcher getMethodMatcher() {
                        return DefaultMethodNameMatcher.byNameAndArgs("execute",
                                                                      new String[]{"java.lang.String",
                                                                          "org.springframework.http.HttpMethod",
                                                                          "org.springframework.web.client.RequestCallback",
                                                                          "org.springframework.web.client.ResponseExtractor<T>",
                                                                          "{}"});
                    }
                }, new IMethodPointCut() {
                    @Override
                    public AgentClassMatcher getClassMatcher() {
                        return DefaultClassNameMatcher.byName("org.springframework.web.client.RestTemplate$AcceptHeaderRequestCallback");
                    }

                    @Override
                    public AgentMethodMatcher getMethodMatcher() {
                        return DefaultMethodNameMatcher.byNameAndArgs("doWithRequest",
                                                                      new String[]{"org.springframework.web.client.ClientHttpRequest"});
                    }
                }

                };
            }
        }, new IAgentHandler() {
            @Override
            public String getHandlerClass() {
                return "com.keruyun.commons.agent.plugin.springmvc.RequestHandler";
            }

            @Override
            public IMethodPointCut[] getPointcuts() {
                return new IMethodPointCut[]{

                    new IMethodPointCut() {
                        @Override
                        public AgentClassMatcher getClassMatcher() {
                            return DefaultClassNameMatcher.byName("org.springframework.http.client.AbstractClientHttpRequest");
                        }

                        @Override
                        public AgentMethodMatcher getMethodMatcher() {
                            return DefaultMethodNameMatcher.byNameAndEmptyArgs("getHeaders");
                        }
                    }

                };
            }
        }, new IAgentHandler() {
            @Override
            public String getHandlerClass() {
                return "com.keruyun.commons.agent.plugin.springmvc.FeignRequestHandler";
            }

            @Override
            public IMethodPointCut[] getPointcuts() {
                return new IMethodPointCut[]{

                    new IMethodPointCut() {
                        @Override
                        public AgentClassMatcher getClassMatcher() {
                            return DefaultClassNameMatcher.byName("org.springframework.cloud.netflix.feign.ribbon.LoadBalancerFeignClient");
                        }

                        @Override
                        public AgentMethodMatcher getMethodMatcher() {
                            return DefaultMethodNameMatcher.byNameAndArgs("execute",
                                                                          new String[]{"feign.Request",
                                                                              "feign.Request$Options"});
                        }
                    }

                };
            }
        },
//                FeignPropRequestHandler操作UnmodifiableMap报错，由于Trace不再使用，先屏蔽掉该Handler
//                new AgentHandler() {
//                    @Override
//                    public String getHandlerClass() {
//                        return "com.keruyun.commons.agent.plugin.springmvc.FeignPropRequestHandler";
//                    }
//
//                    @Override
//                    public Map<String, String> getProperties() {
//                        return null;
//                    }
//
//                    @Override
//                    public AgentPointcut[] getPointcuts() {
//                        return new AgentPointcut[]{
//
//                                new AgentPointcut() {
//                                    @Override
//                                    public AgentClassMatcher getClassMatcher() {
//                                        return DefaultClassNameMatcher.byName("feign.Client$Default");
//                                    }
//
//                                    @Override
//                                    public AgentMethodMatcher getMethodMatcher() {
//                                        return DefaultMethodNameMatcher.byNameAndArgs("execute", new String[]{"feign.Request","feign.Request$Options"});
//                                    }
//                                }
//
//                        };
//                    }
//                }
        };
    }
}
