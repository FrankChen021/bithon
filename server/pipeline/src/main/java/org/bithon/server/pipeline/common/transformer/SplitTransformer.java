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
import org.bithon.server.datasource.input.IInputRow;
import org.bithon.server.datasource.input.PathExpression;

/**
 * @author frank.chen021@outlook.com
 * @date 11/4/22 11:52 PM
 */
public class SplitTransformer extends AbstractTransformer {

    @Getter
    private final String source;

    @Getter
    private final String by;

    @Getter
    private final String[] targets;

    @JsonIgnore
    private final PathExpression pathExpression;

    @JsonCreator
    public SplitTransformer(@JsonProperty("source") String source,
                            @JsonProperty("by") String by,
                            @JsonProperty("targets") String[] targets,
                            @JsonProperty("where") String where) {
        super(where);

        this.source = Preconditions.checkArgumentNotNull("source", source);
        this.by = Preconditions.checkArgumentNotNull("by", by);
        this.targets = Preconditions.checkArgumentNotNull("targets", targets);

        this.pathExpression = PathExpression.Builder.build(this.source);
    }

    @Override
    protected TransformResult transformInternal(IInputRow row) {
        Object val = pathExpression.evaluate(row);
        if (!(val instanceof String)) {
            return TransformResult.CONTINUE;
        }

        String input = val.toString();

        int targetIndex = 0;
        int matchStart = 0;
        int matchIndex = input.indexOf(by);
        while (matchIndex != -1 && targetIndex < targets.length) {
            row.updateColumn(targets[targetIndex++], input.substring(matchStart, matchIndex));
            matchStart = matchIndex + by.length();
            matchIndex = input.indexOf(by, matchStart);
        }
        if (matchStart != 0 && targetIndex < targets.length) {
            row.updateColumn(targets[targetIndex], input.substring(matchStart));
        }

        return TransformResult.CONTINUE;
    }
}
