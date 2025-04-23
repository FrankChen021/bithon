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

package org.bithon.server.storage.jdbc.common.statement.builder;


import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IdentifierExpression;

import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 22/4/25 10:09 pm
 */
class Aggregators {
    final List<Aggregator> aggregators = new ArrayList<>();

    /**
     * Add an aggregation.
     * This also checks there's no same aggregation performed on the same column
     */
    public void add(FunctionExpression aggregatorFunctionCallExpression, String output) {
        if (!contains(aggregatorFunctionCallExpression)) {
            aggregators.add(new Aggregator(aggregatorFunctionCallExpression, output));
        }
    }

    private boolean contains(FunctionExpression rhs) {
        return aggregators.stream()
                          .anyMatch(lhs -> {
                              if (!lhs.isSimpleAggregation) {
                                  return false;
                              }
                              boolean isRhsSimpleAggregation = rhs.getArgs().isEmpty() || rhs.getArgs().get(0) instanceof IdentifierExpression;
                              if (!isRhsSimpleAggregation) {
                                  return false;
                              }

                              FunctionExpression lhsFunction = lhs.aggregateFunction;
                              if (!lhsFunction.getName().equals(rhs.getName())) {
                                  return false;
                              }

                              if (lhsFunction.getArgs().size() != rhs.getArgs().size()) {
                                  return false;
                              }

                              if (lhsFunction.getArgs().isEmpty()) {
                                  return true;
                              }

                              String lhsCol = ((IdentifierExpression) lhs.aggregateFunction.getArgs().get(0)).getIdentifier();
                              return lhsCol.equals(((IdentifierExpression) rhs.getArgs().get(0)).getIdentifier());
                          });
    }

    public int size() {
        return aggregators.size();
    }

    public Aggregator get(int index) {
        return aggregators.get(index);
    }
}
