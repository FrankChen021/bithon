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

package org.bithon.server.agent.controller.rbac;

/**
 * @author frank.chen021@outlook.com
 * @date 22/1/25 5:59 pm
 */
public enum Operation {
    READ {
        @Override
        public boolean isPermitted(Operation operation) {
            return operation == READ || operation == RW;
        }
    },
    WRITE {
        @Override
        public boolean isPermitted(Operation operation) {
            return operation == WRITE || operation == RW;
        }
    },
    RW {
        @Override
        public boolean isPermitted(Operation operation) {
            return true;
        }
    };

    public abstract boolean isPermitted(Operation operation);
}
