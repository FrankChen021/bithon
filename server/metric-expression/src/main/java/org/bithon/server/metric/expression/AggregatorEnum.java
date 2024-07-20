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

package org.bithon.server.metric.expression;

import org.bithon.server.storage.datasource.column.IColumn;
import org.bithon.server.storage.datasource.column.StringColumn;

/**
 * @author Frank Chen
 * @date 24/4/22 3:46 PM
 */
public enum AggregatorEnum {
    avg {
        @Override
        public boolean isColumnSupported(IColumn column) {
            return !(column instanceof StringColumn);
        }
    },
    count {
        @Override
        public boolean isColumnSupported(IColumn column) {
            // Count aggregator can be performed on any types of columns
            return true;
        }
    },
    first {
        @Override
        public boolean isColumnSupported(IColumn column) {
            return !(column instanceof StringColumn);
        }
    },
    last {
        @Override
        public boolean isColumnSupported(IColumn column) {
            return !(column instanceof StringColumn);
        }
    },
    max {
        @Override
        public boolean isColumnSupported(IColumn column) {
            return !(column instanceof StringColumn);
        }
    },
    min {
        @Override
        public boolean isColumnSupported(IColumn column) {
            return !(column instanceof StringColumn);
        }
    },
    sum {
        @Override
        public boolean isColumnSupported(IColumn column) {
            return !(column instanceof StringColumn);
        }
    };

    public abstract boolean isColumnSupported(IColumn column);
}
