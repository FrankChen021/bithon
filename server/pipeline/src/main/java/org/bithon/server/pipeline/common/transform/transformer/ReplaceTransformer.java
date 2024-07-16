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

package org.bithon.server.pipeline.common.transform.transformer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.InputRowAccessor;
import org.bithon.server.storage.datasource.input.PathExpression;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Frank Chen
 * @date 12/3/24 6:35 pm
 */
public class ReplaceTransformer extends AbstractTransformer {

    @Getter
    private final String source;

    @Getter
    private final String find;

    @Getter
    private final String replacement;

    @JsonIgnore
    private final Pattern pattern;
    @JsonIgnore
    private final String quotedReplacement;

    @JsonIgnore
    private final PathExpression pathExpression;

    @JsonIgnore
    private final InputRowAccessor.ISetter valueSetter;

    @JsonCreator
    public ReplaceTransformer(@JsonProperty("source") String source,
                              @JsonProperty("find") String find,
                              @JsonProperty("replacement") String replacement,
                              @JsonProperty("where") String where) {
        super(where);

        this.source = Preconditions.checkArgumentNotNull("source", source);
        this.find = Preconditions.checkArgumentNotNull("find", find);
        this.replacement = Preconditions.checkArgumentNotNull("replacement", replacement);

        this.pathExpression = PathExpression.Builder.build(source);
        this.valueSetter = InputRowAccessor.createSetter(source);

        // See the String.replace(String, String) to know more
        this.pattern = Pattern.compile(this.find, Pattern.LITERAL);
        this.quotedReplacement = Matcher.quoteReplacement(replacement);
    }

    @Override
    protected TransformResult transformInternal(IInputRow data) {
        Object value = pathExpression.evaluate(data);
        if (!(value instanceof String)) {
            return TransformResult.CONTINUE;
        }

        String replaced = this.pattern.matcher((String) value).replaceAll(this.quotedReplacement);
        valueSetter.set(data, replaced);
        return TransformResult.CONTINUE;
    }
}
