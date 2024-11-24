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

import org.bithon.component.commons.expression.IExpression;

/**
 * <p>
 * Absolute comparison
 * avg by (a) (data-source.metric{dim1 = 'x'}) > 0.5
 * <p>
 * Relative comparison with an absolute value
 * avg by (a) (data-source.metric{dim1 = 'z', dim2=''}[1m|h]) > 5[-60m]
 * <p>
 * Relative comparison with a percentage
 * avg by (a) (data-source.metric{dim1 = 'a', dim2=''}[1m|h]) > 5%[-1d]
 * avg by (a) (data-source.metric{dim1 = 'b', dim2=''}[1m|h]) > 5%['2023-01-01']
 * <p>
 *
 * @author frankchen
 * @date 2020-08-21 14:56:50
 */
public interface MetricQLExpression extends IExpression {
}
