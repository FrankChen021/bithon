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

package org.bithon.agent;

/**
 * @author frank.chen021@outlook.com
 */
public final class AgentBuildVersion {

    public static final String VERSION = "${project.version}";

    /**
     * SCM(git) revision
     */
    public static final String SCM_REVISION = "${buildNumber}";

    /**
     * SCM branch
     */
    public static final String SCM_BRANCH = "${scmBranch}";

    /**
     * build timestamp
     */
    public static final String TIMESTAMP = "${buildtimestamp}";
}
