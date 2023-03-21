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

package org.bithon.agent.instrumentation.logging;

import org.bithon.agent.instrumentation.expt.AgentException;
import org.bithon.agent.instrumentation.loader.AgentClassLoader;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Logger used for classes in the agent-instrumentation
 *
 * @author frank.chen021@outlook.com
 * @date 2021/2/19 8:26 下午
 */
public class LoggerFactory {

    public static ILogger getLogger(Class<?> logClass) {
        // The logger is provided in the agent-core, use reflection to instantiate a class
        String loggerName = "org.bithon.agent.core.logging.LoggerAdaptor";

        try {
            Class<?> loggerClass = Class.forName(loggerName,
                                                 true,
                                                 AgentClassLoader.getClassLoader());
            Method getLoggerMethod = loggerClass.getDeclaredMethod("getLogger", Class.class);
            return (ILogger) getLoggerMethod.invoke(null, logClass);
        } catch (ClassNotFoundException e) {
            System.out.printf(Locale.ENGLISH, "[%s] could not be found, AopLogger falls back to Console Logger%n", loggerName);
            return new ILogger() {
                @Override
                public void info(String messageFormat, Object... args) {
                    System.out.printf(Locale.ENGLISH, messageFormat, args);
                }

                @Override
                public void warn(String messageFormat, Object... args) {
                    System.err.printf(Locale.ENGLISH, messageFormat, args);
                }

                @Override
                public void warn(String message, Throwable e) {
                    System.err.printf(Locale.ENGLISH, "[WARN] %s: %s%n", message, e.toString());
                }

                @Override
                public void error(String message, Throwable e) {
                    System.err.printf(Locale.ENGLISH, "[ERROR] %s: %s%n", message, e.toString());
                }

                @Override
                public void error(String messageFormat, Object... args) {
                    System.err.printf(Locale.ENGLISH, "[ERROR] %s%n", String.format(Locale.ENGLISH, messageFormat, args));
                }
            };
        } catch (Exception e) {
            throw new AgentException(e);
        }
    }
}
