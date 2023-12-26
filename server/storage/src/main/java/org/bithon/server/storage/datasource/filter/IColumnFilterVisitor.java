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

package org.bithon.server.storage.datasource.filter;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/12/26 13:30
 */
public interface IColumnFilterVisitor<T> {
    T visit(IColumnFilter.GreaterThanFilter filter);

    T visit(IColumnFilter.GreaterThanOrEqualFilter filter);

    T visit(IColumnFilter.EqualFilter filter);

    T visit(IColumnFilter.LessThanFilter filter);

    T visit(IColumnFilter.LessThanOrEqualFilter filter);

    T visit(IColumnFilter.NotEqualFilter filter);
}
