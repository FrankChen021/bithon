package com.sbss.bithon.agent.plugin.mysql;

import com.sbss.bithon.agent.core.transformer.AbstractClassTransformer;
import com.sbss.bithon.agent.core.transformer.matcher.DefaultMethodNameMatcher;
import com.sbss.bithon.agent.core.transformer.plugin.AgentHandler;
import com.sbss.bithon.agent.core.transformer.plugin.IAgentHandler;
import com.sbss.bithon.agent.core.transformer.plugin.MethodPointCut;

public class MySqlTransformer extends AbstractClassTransformer {
    private static final String MYSQL_PLUGIN_STATEMENT_HANDLER = "com.sbss.bithon.agent.plugin.mysql.StatementHandler";
    private static final String MYSQL_PLUGIN_PREPAREDSTATEMENT_HANDLER = "com.sbss.bithon.agent.plugin.mysql.PreparedStatementHandler";
    private static final String MYSQL_PLUGIN_IO_HANDLER = "com.sbss.bithon.agent.plugin.mysql.IoHandler";

    private static final String OLD_VERSION_PREPARED_STATEMENT_CLASS = "com.mysql.jdbc.PreparedStatement";
    private static final String NEW_VERSION_PREPARED_STATEMENT_CLASS = "com.mysql.cj.jdbc.PreparedStatement";
    private static final String OLD_VERSION_CONNECTION_CLASS = "com.mysql.jdbc.ConnectionImpl";
    private static final String NEW_VERSION_CONNECTION_CLASS = "com.mysql.cj.jdbc.ConnectionImpl";
    private static final String OLD_VERSION_STATEMENT_CLASS = "com.mysql.jdbc.StatementImpl";
    private static final String NEW_VERSION_STATEMENT_CLASS = "com.mysql.cj.jdbc.StatementImpl";
    private static final String MYSQL_IO_CLASS = "com.mysql.jdbc.MysqlIO";
    static final String METHOD_EXECUTE = "execute";
    private static final String METHOD_EXECUTE_QUERY = "executeQuery";
    static final String METHOD_EXECUTE_UPDATE = "executeUpdate";
    static final String METHOD_EXECUTE_INTERNAL = "executeInternal";
    static final String METHOD_EXECUTE_UPDATE_INTERNAL = "executeUpdateInternal";
    static final String METHOD_SEND_COMMAND = "sendCommand";
    private static final String METHOD_READ_ALL_RESULTS = "readAllResults";

    private static final String[] STATEMENT_EXECUTE_ARGUMENTS = new String[]{"java.lang.String", "boolean"};
    private static final String[] STATEMENT_EXECUTE_QUERY_ARGUMENTS = new String[]{"java.lang.String"};
    private static final String[] STATEMENT_EXECUTE_UPDATE_ARGUMENTS = new String[]{"java.lang.String", "boolean",
        "boolean"};

    private static final String[] MYSQL_IO_SEND_COMMAND_ARGUMENTS = new String[]{"int", "java.lang.String",
        "com.mysql.jdbc.Buffer", "boolean",
        "java.lang.String", "int"};
    private static final String[] MYSQL_IO_READ_ALL_RESULTS_ARGUMENTS = new String[]{"com.mysql.jdbc.StatementImpl",
        "int", "int", "int", "boolean",
        "java.lang.String",
        "com.mysql.jdbc.Buffer",
        "boolean", "long",
        "[Lcom.mysql.jdbc.Field;"};

    @Override
    public IAgentHandler[] getHandlers() {
        return new IAgentHandler[]{new AgentHandler(MYSQL_PLUGIN_PREPAREDSTATEMENT_HANDLER,
                                                    new MethodPointCut(OLD_VERSION_PREPARED_STATEMENT_CLASS,
                                                                       DefaultMethodNameMatcher.byNameAndEmptyArgs(METHOD_EXECUTE)),
                                                    new MethodPointCut(OLD_VERSION_PREPARED_STATEMENT_CLASS,
                                                                       DefaultMethodNameMatcher.byNameAndEmptyArgs(METHOD_EXECUTE_QUERY)),
                                                    new MethodPointCut(OLD_VERSION_PREPARED_STATEMENT_CLASS,
                                                                       DefaultMethodNameMatcher.byNameAndEmptyArgs(METHOD_EXECUTE_UPDATE)),
                                                    new MethodPointCut(NEW_VERSION_PREPARED_STATEMENT_CLASS,
                                                                       DefaultMethodNameMatcher.byNameAndEmptyArgs(METHOD_EXECUTE)),
                                                    new MethodPointCut(NEW_VERSION_PREPARED_STATEMENT_CLASS,
                                                                       DefaultMethodNameMatcher.byNameAndEmptyArgs(METHOD_EXECUTE_QUERY)),
                                                    new MethodPointCut(NEW_VERSION_PREPARED_STATEMENT_CLASS,
                                                                       DefaultMethodNameMatcher.byNameAndEmptyArgs(METHOD_EXECUTE_UPDATE))),

            new AgentHandler(MYSQL_PLUGIN_IO_HANDLER,
                             new MethodPointCut(MYSQL_IO_CLASS,
                                                DefaultMethodNameMatcher.byNameAndArgs(METHOD_SEND_COMMAND,
                                                                                       MYSQL_IO_SEND_COMMAND_ARGUMENTS)),
                             new MethodPointCut(MYSQL_IO_CLASS,
                                                DefaultMethodNameMatcher.byNameAndArgs(METHOD_READ_ALL_RESULTS,
                                                                                       MYSQL_IO_READ_ALL_RESULTS_ARGUMENTS))),

            new AgentHandler(MYSQL_PLUGIN_STATEMENT_HANDLER,
                             new MethodPointCut(OLD_VERSION_STATEMENT_CLASS,
                                                DefaultMethodNameMatcher.byNameAndArgs(METHOD_EXECUTE_INTERNAL,
                                                                                       STATEMENT_EXECUTE_ARGUMENTS)),
                             new MethodPointCut(OLD_VERSION_STATEMENT_CLASS,
                                                DefaultMethodNameMatcher.byNameAndArgs(METHOD_EXECUTE_QUERY,
                                                                                       STATEMENT_EXECUTE_QUERY_ARGUMENTS)),
                             new MethodPointCut(OLD_VERSION_STATEMENT_CLASS,
                                                DefaultMethodNameMatcher.byNameAndArgs(METHOD_EXECUTE_UPDATE,
                                                                                       STATEMENT_EXECUTE_UPDATE_ARGUMENTS)),
                             new MethodPointCut(OLD_VERSION_STATEMENT_CLASS,
                                                DefaultMethodNameMatcher.byNameAndArgs(METHOD_EXECUTE_UPDATE_INTERNAL,
                                                                                       STATEMENT_EXECUTE_UPDATE_ARGUMENTS)),
                             new MethodPointCut(NEW_VERSION_STATEMENT_CLASS,
                                                DefaultMethodNameMatcher.byNameAndArgs(METHOD_EXECUTE_INTERNAL,
                                                                                       STATEMENT_EXECUTE_ARGUMENTS)),
                             new MethodPointCut(NEW_VERSION_STATEMENT_CLASS,
                                                DefaultMethodNameMatcher.byNameAndArgs(METHOD_EXECUTE_QUERY,
                                                                                       STATEMENT_EXECUTE_QUERY_ARGUMENTS)),
                             new MethodPointCut(NEW_VERSION_STATEMENT_CLASS,
                                                DefaultMethodNameMatcher.byNameAndArgs(METHOD_EXECUTE_UPDATE,
                                                                                       STATEMENT_EXECUTE_UPDATE_ARGUMENTS)),
                             new MethodPointCut(NEW_VERSION_STATEMENT_CLASS,
                                                DefaultMethodNameMatcher.byNameAndArgs(METHOD_EXECUTE_UPDATE_INTERNAL,
                                                                                       STATEMENT_EXECUTE_UPDATE_ARGUMENTS))),

            new AgentHandler("com.sbss.bithon.agent.plugin.mysql.ConnectionTraceHandler",
                             new MethodPointCut(OLD_VERSION_CONNECTION_CLASS,
                                                DefaultMethodNameMatcher.byNameAndArgs("prepareStatement",
                                                                                       "java.lang.String")),
                             new MethodPointCut(OLD_VERSION_CONNECTION_CLASS,
                                                DefaultMethodNameMatcher.byNameAndArgs("prepareStatement",
                                                                                       "java.lang.String",
                                                                                       "int")),
                             new MethodPointCut(OLD_VERSION_CONNECTION_CLASS,
                                                DefaultMethodNameMatcher.byNameAndArgs("prepareStatement",
                                                                                       "java.lang.String",
                                                                                       "[I")),
                             new MethodPointCut(OLD_VERSION_CONNECTION_CLASS,
                                                DefaultMethodNameMatcher.byNameAndArgs("prepareStatement",
                                                                                       "java.lang.String",
                                                                                       "[Ljava.lang.String;")),
                             new MethodPointCut(OLD_VERSION_CONNECTION_CLASS,
                                                DefaultMethodNameMatcher.byNameAndArgs("prepareStatement",
                                                                                       "java.lang.String",
                                                                                       "int",
                                                                                       "int")),
                             new MethodPointCut(OLD_VERSION_CONNECTION_CLASS,
                                                DefaultMethodNameMatcher.byNameAndArgs("prepareStatement",
                                                                                       "java.lang.String",
                                                                                       "int",
                                                                                       "int",
                                                                                       "int")),
                             new MethodPointCut(NEW_VERSION_CONNECTION_CLASS,
                                                DefaultMethodNameMatcher.byNameAndArgs("prepareStatement",
                                                                                       "java.lang.String")),
                             new MethodPointCut(NEW_VERSION_CONNECTION_CLASS,
                                                DefaultMethodNameMatcher.byNameAndArgs("prepareStatement",
                                                                                       "java.lang.String",
                                                                                       "int")),
                             new MethodPointCut(NEW_VERSION_CONNECTION_CLASS,
                                                DefaultMethodNameMatcher.byNameAndArgs("prepareStatement",
                                                                                       "java.lang.String",
                                                                                       "[I")),
                             new MethodPointCut(NEW_VERSION_CONNECTION_CLASS,
                                                DefaultMethodNameMatcher.byNameAndArgs("prepareStatement",
                                                                                       "java.lang.String",
                                                                                       "[Ljava.lang.String;")),
                             new MethodPointCut(NEW_VERSION_CONNECTION_CLASS,
                                                DefaultMethodNameMatcher.byNameAndArgs("prepareStatement",
                                                                                       "java.lang.String",
                                                                                       "int",
                                                                                       "int")),
                             new MethodPointCut(NEW_VERSION_CONNECTION_CLASS,
                                                DefaultMethodNameMatcher.byNameAndArgs("prepareStatement",
                                                                                       "java.lang.String",
                                                                                       "int",
                                                                                       "int",
                                                                                       "int")))};

//                new AgentHandler() {
//                    @Override
//                    public String getHandlerClass() {
//                        return "com.sbss.bithon.agent.plugin.mysql.StatementTraceHandler";
//                    }
//
//                    @Override
//                    public Map<String, String> getProperties() {
//                        Map<String, String> properties = new HashMap<>();
//                        properties.put("ignoredSuffixes", "select @@session.tx_read_only");
//                        return properties;
//                    }
//
//                    @Override
//                    public AgentPointcut[] getPointcuts() {
//                        return new AgentPointcut[]{
//                                new AgentPointcut() {
//                                    @Override
//                                    public AgentClassMatcher getClassMatcher() {
//                                        return DefaultClassNameMatcher.byName(OLD_VERSION_STATEMENT_CLASS);
//                                    }
//
//                                    @Override
//                                    public AgentMethodMatcher getMethodMatcher() {
//                                        return DefaultMethodNameMatcher.byNameAndArgs(METHOD_EXECUTE, new String[]{"java.lang.String"});
//                                    }
//                                },
//                                new AgentPointcut() {
//                                    @Override
//                                    public AgentClassMatcher getClassMatcher() {
//                                        return DefaultClassNameMatcher.byName(NEW_VERSION_STATEMENT_CLASS);
//                                    }
//
//                                    @Override
//                                    public AgentMethodMatcher getMethodMatcher() {
//                                        return DefaultMethodNameMatcher.byNameAndArgs(METHOD_EXECUTE, new String[]{"java.lang.String"});
//                                    }
//                                },
//
//                                new AgentPointcut() {
//                                    @Override
//                                    public AgentClassMatcher getClassMatcher() {
//                                        return DefaultClassNameMatcher.byName(OLD_VERSION_STATEMENT_CLASS);
//                                    }
//
//                                    @Override
//                                    public AgentMethodMatcher getMethodMatcher() {
//                                        return DefaultMethodNameMatcher.byNameAndArgs(METHOD_EXECUTE_QUERY, STATEMENT_EXECUTE_QUERY_ARGUMENTS);
//                                    }
//                                },
//                                new AgentPointcut() {
//                                    @Override
//                                    public AgentClassMatcher getClassMatcher() {
//                                        return DefaultClassNameMatcher.byName(OLD_VERSION_STATEMENT_CLASS);
//                                    }
//
//                                    @Override
//                                    public AgentMethodMatcher getMethodMatcher() {
//                                        return DefaultMethodNameMatcher.byNameAndArgs(METHOD_EXECUTE_UPDATE, STATEMENT_EXECUTE_UPDATE_ARGUMENTS);
//                                    }
//                                },
//
//                                new AgentPointcut() {
//                                    @Override
//                                    public AgentClassMatcher getClassMatcher() {
//                                        return DefaultClassNameMatcher.byName(OLD_VERSION_STATEMENT_CLASS);
//                                    }
//
//                                    @Override
//                                    public AgentMethodMatcher getMethodMatcher() {
//                                        return DefaultMethodNameMatcher.byNameAndArgs(METHOD_EXECUTE_UPDATE_INTERNAL, STATEMENT_EXECUTE_UPDATE_ARGUMENTS);
//                                    }
//                                },
//
//                                new AgentPointcut() {
//                                    @Override
//                                    public AgentClassMatcher getClassMatcher() {
//                                        return DefaultClassNameMatcher.byName(NEW_VERSION_STATEMENT_CLASS);
//                                    }
//
//                                    @Override
//                                    public AgentMethodMatcher getMethodMatcher() {
//                                        return DefaultMethodNameMatcher.byNameAndArgs(METHOD_EXECUTE_INTERNAL, STATEMENT_EXECUTE_ARGUMENTS);
//                                    }
//                                },
//
//                                new AgentPointcut() {
//                                    @Override
//                                    public AgentClassMatcher getClassMatcher() {
//                                        return DefaultClassNameMatcher.byName(NEW_VERSION_STATEMENT_CLASS);
//                                    }
//
//                                    @Override
//                                    public AgentMethodMatcher getMethodMatcher() {
//                                        return DefaultMethodNameMatcher.byNameAndArgs(METHOD_EXECUTE_QUERY, STATEMENT_EXECUTE_QUERY_ARGUMENTS);
//                                    }
//                                },
//
//                                new AgentPointcut() {
//                                    @Override
//                                    public AgentClassMatcher getClassMatcher() {
//                                        return DefaultClassNameMatcher.byName(NEW_VERSION_STATEMENT_CLASS);
//                                    }
//
//                                    @Override
//                                    public AgentMethodMatcher getMethodMatcher() {
//                                        return DefaultMethodNameMatcher.byNameAndArgs(METHOD_EXECUTE_UPDATE, STATEMENT_EXECUTE_UPDATE_ARGUMENTS);
//                                    }
//                                },
//
//                                new AgentPointcut() {
//                                    @Override
//                                    public AgentClassMatcher getClassMatcher() {
//                                        return DefaultClassNameMatcher.byName(NEW_VERSION_STATEMENT_CLASS);
//                                    }
//
//                                    @Override
//                                    public AgentMethodMatcher getMethodMatcher() {
//                                        return DefaultMethodNameMatcher.byNameAndArgs(METHOD_EXECUTE_UPDATE_INTERNAL, STATEMENT_EXECUTE_UPDATE_ARGUMENTS);
//                                    }
//                                }
//                        };
//                    }
//                },
//                new AgentHandler() {
//                    @Override
//                    public String getHandlerClass() {
//                        return "com.sbss.bithon.agent.plugin.mysql.PreparedStatementTraceHandler";
//                    }
//
//                    @Override
//                    public Map<String, String> getProperties() {
//                        Map<String, String> properties = new HashMap<>();
//                        properties.put("ignoredSuffixes", "select @@session.tx_read_only");
//                        return properties;
//                    }
//
//                    @Override
//                    public AgentPointcut[] getPointcuts() {
//                        return new AgentPointcut[]{
//                                // 旧版本mysql prepared statement 拦截
//                                new AgentPointcut() {
//                                    @Override
//                                    public AgentClassMatcher getClassMatcher() {
//                                        return DefaultClassNameMatcher.byName(OLD_VERSION_PREPARED_STATEMENT_CLASS);
//                                    }
//
//                                    @Override
//                                    public AgentMethodMatcher getMethodMatcher() {
//                                        return DefaultMethodNameMatcher.byNameAndArgs(METHOD_EXECUTE, null);
//                                    }
//                                },
//
//                                new AgentPointcut() {
//                                    @Override
//                                    public AgentClassMatcher getClassMatcher() {
//                                        return DefaultClassNameMatcher.byName(OLD_VERSION_PREPARED_STATEMENT_CLASS);
//                                    }
//
//                                    @Override
//                                    public AgentMethodMatcher getMethodMatcher() {
//                                        return DefaultMethodNameMatcher.byNameAndArgs(METHOD_EXECUTE_QUERY, null);
//                                    }
//                                },
//
//                                new AgentPointcut() {
//                                    @Override
//                                    public AgentClassMatcher getClassMatcher() {
//                                        return DefaultClassNameMatcher.byName(OLD_VERSION_PREPARED_STATEMENT_CLASS);
//                                    }
//
//                                    @Override
//                                    public AgentMethodMatcher getMethodMatcher() {
//                                        return DefaultMethodNameMatcher.byNameAndArgs(METHOD_EXECUTE_UPDATE, null);
//                                    }
//                                },
//
//                                // 新版本mysql prepared statement 拦截
//                                new AgentPointcut() {
//                                    @Override
//                                    public AgentClassMatcher getClassMatcher() {
//                                        return DefaultClassNameMatcher.byName(NEW_VERSION_PREPARED_STATEMENT_CLASS);
//                                    }
//
//                                    @Override
//                                    public AgentMethodMatcher getMethodMatcher() {
//                                        return DefaultMethodNameMatcher.byNameAndArgs(METHOD_EXECUTE, null);
//                                    }
//                                },
//
//                                new AgentPointcut() {
//                                    @Override
//                                    public AgentClassMatcher getClassMatcher() {
//                                        return DefaultClassNameMatcher.byName(NEW_VERSION_PREPARED_STATEMENT_CLASS);
//                                    }
//
//                                    @Override
//                                    public AgentMethodMatcher getMethodMatcher() {
//                                        return DefaultMethodNameMatcher.byNameAndArgs(METHOD_EXECUTE_QUERY, null);
//                                    }
//                                },
//
//                                new AgentPointcut() {
//                                    @Override
//                                    public AgentClassMatcher getClassMatcher() {
//                                        return DefaultClassNameMatcher.byName(NEW_VERSION_PREPARED_STATEMENT_CLASS);
//                                    }
//
//                                    @Override
//                                    public AgentMethodMatcher getMethodMatcher() {
//                                        return DefaultMethodNameMatcher.byNameAndArgs(METHOD_EXECUTE_UPDATE, null);
//                                    }
//                                }
//                        };
//                    }
//                },

    }
}
