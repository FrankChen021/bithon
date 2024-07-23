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

package org.bithon.server.storage.datasource.column;

import org.bithon.server.storage.datasource.column.aggregatable.count.AggregateCountColumn;
import org.bithon.server.storage.datasource.column.aggregatable.last.AggregateDoubleLastColumn;
import org.bithon.server.storage.datasource.column.aggregatable.last.AggregateLongLastColumn;
import org.bithon.server.storage.datasource.column.aggregatable.max.AggregateLongMaxColumn;
import org.bithon.server.storage.datasource.column.aggregatable.min.AggregateLongMinColumn;
import org.bithon.server.storage.datasource.column.aggregatable.sum.AggregateDoubleSumColumn;
import org.bithon.server.storage.datasource.column.aggregatable.sum.AggregateLongSumColumn;

/**
 * @author frank.chen021@outlook.com
 * @date 23/7/24 1:32 pm
 */
public interface IColumnVisitor<T> {
    T visit(LongColumn column);
    T visit(ObjectColumn column);
    T visit(StringColumn column);
    T visit(DateTimeColumn column);
    T visit(ExpressionColumn column);
    T visit(AggregateDoubleSumColumn column);
    T visit(AggregateLongSumColumn column);
    T visit(AggregateLongMinColumn column);
    T visit(AggregateDoubleLastColumn column);
    T visit(AggregateLongMaxColumn column);
    T visit(AggregateLongLastColumn column);
    T visit(AggregateCountColumn column);
}
