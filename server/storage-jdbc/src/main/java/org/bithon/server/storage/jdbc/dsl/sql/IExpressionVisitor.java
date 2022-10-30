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

package org.bithon.server.storage.jdbc.dsl.sql;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/9/4 15:05
 */
public interface IExpressionVisitor {

    void before(SelectExpression selectExpression);
    void visit(SelectExpression selectExpression);
    void after(SelectExpression selectExpression);

    void visit(OrderByExpression orderByExpression);

    void visit(TableExpression table);

    void visit(WhereExpression whereExpression);

    void visit(GroupByExpression groupByExpression);

    void visit(FromExpression fromExpression);

    void visit(AliasExpression aliasExpression);

    void visit(NameExpression nameExpression);

    void before(FunctionExpression functionExpression);

    void after(FunctionExpression functionExpression);

    void visit(StringExpression stringExpression);

    void visit(FieldsExpression fieldsExpression);

    void visit(LimitExpression limitExpression);
}
