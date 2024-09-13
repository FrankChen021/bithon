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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/9/4 14:58
 */
public class GroupByClause implements IASTNode {
    @Getter
    private final List<String> fields = new ArrayList<>(2);

    public GroupByClause addField(String field) {
        this.fields.add(field);
        return this;
    }

    public GroupByClause addFields(Collection<String> fields) {
        if (fields != null) {
            this.fields.addAll(fields);
        }
        return this;
    }

    public boolean isEmpty() {
        return fields.isEmpty();
    }

    @Override
    public void accept(IASTNodeVisitor visitor) {
        visitor.visit(this);
    }
}
