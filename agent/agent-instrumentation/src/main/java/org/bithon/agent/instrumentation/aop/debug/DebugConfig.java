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

package org.bithon.agent.instrumentation.aop.debug;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/8/7 13:53
 */
public class DebugConfig {
    private boolean outputClassFile = false;
    private boolean profiling = false;

    public boolean isOutputClassFile() {
        return outputClassFile;
    }

    public void setOutputClassFile(boolean outputClassFile) {
        this.outputClassFile = outputClassFile;
    }

    public boolean isProfiling() {
        return profiling;
    }

    public void setProfiling(boolean profiling) {
        this.profiling = profiling;
    }
}
