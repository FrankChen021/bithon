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

package org.bithon.component.commons.expression.expt;

import org.bithon.component.commons.utils.StringUtils;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/7/18 21:05
 */
public class InvalidExpressionException extends RuntimeException {
    public InvalidExpressionException(String messageFormat, Object... args) {
        super(StringUtils.format(messageFormat, args));
    }
}
