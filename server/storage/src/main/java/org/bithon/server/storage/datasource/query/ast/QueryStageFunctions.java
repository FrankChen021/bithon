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

package org.bithon.server.storage.datasource.query.ast;

import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.function.Functions;
import org.bithon.component.commons.expression.function.builtin.AggregateFunction;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/7/28 12:48
 */
@Component
public class QueryStageFunctions implements InitializingBean {

    @Override
    public void afterPropertiesSet() {
        Functions.getInstance().register(new Cardinality());
        Functions.getInstance().register(new Rate());
        Functions.getInstance().register(new GroupConcat());
    }

    public static class Cardinality extends AggregateFunction {
        public Cardinality() {
            super("cardinality");
        }

        @Override
        public void validateParameter(List<IExpression> parameters) {
            Validator.validateParameterSize(1, parameters.size());
        }

        @Override
        public Object evaluate(List<Object> parameters) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
    }

    public static class Rate extends AggregateFunction {
        public Rate() {
            super("rate");
        }

        @Override
        public void validateParameter(List<IExpression> parameters) {
            Validator.validateParameterSize(1, parameters.size());
        }

        @Override
        public Object evaluate(List<Object> parameters) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
    }

    public static class GroupConcat extends AggregateFunction {
        public GroupConcat() {
            super("groupConcat");
        }

        @Override
        public void validateParameter(List<IExpression> parameters) {
            Validator.validateParameterSize(1, parameters.size());
        }

        @Override
        public Object evaluate(List<Object> parameters) {
            throw new UnsupportedOperationException("Not implemented yet");
        }
    }
}
