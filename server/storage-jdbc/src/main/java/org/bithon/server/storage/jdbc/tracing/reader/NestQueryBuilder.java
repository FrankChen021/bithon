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

package org.bithon.server.storage.jdbc.tracing.reader;

import org.bithon.component.commons.expression.IExpression;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.SelectConditionStep;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/5/7 15:41
 */
abstract class NestQueryBuilder {
    protected DSLContext dslContext;
    protected LocalDateTime start;
    protected LocalDateTime end;

    protected SelectConditionStep<Record1<String>> in;

    public NestQueryBuilder dslContext(DSLContext dslContext) {
        this.dslContext = dslContext;
        return this;
    }

    public NestQueryBuilder start(LocalDateTime start) {
        this.start = start;
        return this;
    }

    public NestQueryBuilder end(LocalDateTime end) {
        this.end = end;
        return this;
    }

    public NestQueryBuilder in(SelectConditionStep<Record1<String>> in) {
        this.in = in;
        return this;
    }

    public abstract SelectConditionStep<Record1<String>> build(Map<Integer, IExpression> filters);
}
