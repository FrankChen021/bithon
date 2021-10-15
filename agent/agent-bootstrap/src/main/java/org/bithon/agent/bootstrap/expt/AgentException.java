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

package org.bithon.agent.bootstrap.expt;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/18 8:42 下午
 */
public class AgentException extends RuntimeException {
    public AgentException(String message, Throwable e) {
        super(message, e);
    }

    public AgentException(String message) {
        super(message);
    }

    public AgentException(String format, Object... args) {
        super(String.format(format, args));
    }

    public AgentException(Exception e, String format, Object... args) {
        super(String.format(format, args), e);
    }

    public AgentException(Exception e) {
        super(e);
    }
}
