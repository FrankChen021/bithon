/*
 *    Copyright 2020 bithon.cn
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

package org.bithon.agent.bootstrap.aop;

import org.bithon.agent.bootstrap.expt.AgentException;

import java.lang.reflect.Method;

/**
 * ALL methods in this class will be executed in classes which are loaded by bootstrap class loader
 * So, there must be as LESS dependencies as possible for this class
 *
 * @author frank.chen021@outlook.com
 * @date 2021/2/19 8:26 下午
 */
public class BootstrapHelper {

    private static ClassLoader classLoader;

    public static IAopLogger createAopLogger(Class<?> logClass) {
        String loggerName = "com.sbss.bithon.agent.core.aop.AopLogger";
        try {
            Class<?> loggerClass = Class.forName(loggerName,
                                                 true,
                                                 classLoader);
            Method getLoggerMethod = loggerClass.getDeclaredMethod("getLogger", Class.class);
            return (IAopLogger) getLoggerMethod.invoke(null, logClass);
        } catch (ClassNotFoundException e) {
            System.out.printf("[%s] could not be found, AopLogger falls back to Console Logger\n", loggerName);
            return new IAopLogger() {
                @Override
                public void warn(String message, Throwable e) {
                    System.out.printf("[WARN] %s: %s\n", message, e.toString());
                }

                @Override
                public void error(String message, Throwable e) {
                    System.out.printf("[ERROR] %s: %s\n", message, e.toString());
                }
            };
        } catch (Exception e) {
            throw new AgentException(e);
        }
    }

    public static void setPluginClassLoader(ClassLoader classLoader) {
        BootstrapHelper.classLoader = classLoader;
    }

    public static ClassLoader getPluginClassLoader() {
        return classLoader;
    }
}
