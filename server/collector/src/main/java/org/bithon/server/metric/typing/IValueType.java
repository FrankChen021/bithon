/*
 *    Copyright 2020 bithon.cn
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

package org.bithon.server.metric.typing;

/**
 * @author frank.chen021@outlook.com
 */
public interface IValueType {
    String format(Number value);

    boolean isGreaterThan(Number left, Number right);

    boolean isGreaterThanOrEqual(Number left, Number right);

    boolean isLessThan(Number left, Number right);

    boolean isLessThanOrEqual(Number left, Number right);

    boolean isEqual(Number left, Number right);

    Number diff(Number left, Number right);

    Number scaleTo(Number value, int scale);
}
