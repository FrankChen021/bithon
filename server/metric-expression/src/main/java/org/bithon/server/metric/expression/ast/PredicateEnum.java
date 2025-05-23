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

package org.bithon.server.metric.expression.ast;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/7/20 17:12
 */
public enum PredicateEnum {

    LT {
        @Override
        public String toString() {
            return "<";
        }
    },

    LTE {
        @Override
        public String toString() {
            return "<=";
        }
    },

    GT {
        @Override
        public String toString() {
            return ">";
        }
    },

    GTE {
        @Override
        public String toString() {
            return ">=";
        }
    },

    NE {
        @Override
        public String toString() {
            return "<>";
        }
    },
    EQ {
        @Override
        public String toString() {
            return "=";
        }
    },
    IS_NULL {
        @Override
        public String toString() {
            return "IS NULL";
        }
    }
}
