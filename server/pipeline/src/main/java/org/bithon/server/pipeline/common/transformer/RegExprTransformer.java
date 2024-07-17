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

package org.bithon.server.pipeline.common.transformer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.PathExpression;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author frank.chen021@outlook.com
 * @date 5/8/22 11:43 AM
 */
public class RegExprTransformer extends AbstractTransformer {

    /**
     * source field
     */
    @Getter
    private final String field;

    @Getter
    private final String regexpr;

    @Getter
    private final String[] names;


    @JsonIgnore
    private final Pattern pattern;

    @JsonIgnore
    private final PathExpression pathExpression;

    @JsonCreator
    public RegExprTransformer(@JsonProperty("field") String field,
                              @JsonProperty("regexpr") String regexpr,
                              @JsonProperty("names") String[] names,
                              @JsonProperty("where") String where) {
        super(where);

        this.field = Preconditions.checkArgumentNotNull("field", field);
        this.regexpr = Preconditions.checkArgumentNotNull("regexpr", regexpr);
        this.names = names;

        this.pathExpression = PathExpression.Builder.build(field);
        this.pattern = Pattern.compile(regexpr);
    }

    @Override
    protected TransformResult transformInternal(IInputRow inputRow) {
        Object val = pathExpression.evaluate(inputRow);
        if (val != null) {
            Matcher matcher = this.pattern.matcher(val.toString());
            if (matcher.find() && matcher.groupCount() == names.length) {
                for (int i = 0; i < names.length; i++) {
                    inputRow.updateColumn(names[i], matcher.group(i + 1));
                }
            }
        }

        return TransformResult.CONTINUE;
    }
}
