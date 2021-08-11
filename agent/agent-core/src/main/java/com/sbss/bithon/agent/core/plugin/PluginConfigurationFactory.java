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

package com.sbss.bithon.agent.core.plugin;

import com.sbss.bithon.agent.core.config.ConfigManager;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/8/11 16:31
 */
public class PluginConfigurationFactory {

    public static ConfigManager create(Class<? extends IPlugin> pluginClass) {
        String pkgName = pluginClass.getPackage().getName().replace('.', '/');

        String[] packages = pkgName.split("/");
        String pluginName = packages[packages.length - 1];

        String name = pkgName + "/plugin.yml";
        try (InputStream is = pluginClass.getClassLoader().getResourceAsStream(name)) {
            if (is == null) {
                return ConfigManager.EMPTY;
            }
            return ConfigManager.create(name, is, "bithon.agent.plugin." + pluginName + ".");
        } catch (IOException ignored) {
            // ignore this exception thrown from InputStream.close
            return ConfigManager.EMPTY;
        }
    }
}
