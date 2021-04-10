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

package com.sbss.bithon.agent.core.plugin.precondition;

import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import shaded.net.bytebuddy.description.type.TypeDescription;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/15
 */
public class OrChecker implements IPluginInstallationChecker {
    private final IPluginInstallationChecker[] checkers;

    public OrChecker(IPluginInstallationChecker[] checkers) {
        this.checkers = checkers;
    }

    @Override
    public boolean canInstall(AbstractPlugin plugin, ClassLoader classLoader, TypeDescription typeDescription) {
        for (IPluginInstallationChecker checker : checkers) {
            if (checker.canInstall(plugin, classLoader, typeDescription)) {
                return true;
            }
        }
        return false;
    }
}
