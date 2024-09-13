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

import lombok.Getter;
import org.bithon.component.commons.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/9/4 14:55
 */
@Getter
public class Where implements IASTNode {

    private final List<String> expressions = new ArrayList<>();

    public Where addExpression(String expression) {
        if (!StringUtils.isEmpty(expression)) {
            expressions.add(expression);
        }
        return this;
    }

    @Override
    public void accept(IASTNodeVisitor visitor) {
        visitor.visit(this);
    }
}
