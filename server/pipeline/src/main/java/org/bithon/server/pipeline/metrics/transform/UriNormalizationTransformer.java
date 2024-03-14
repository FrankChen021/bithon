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

package org.bithon.server.pipeline.metrics.transform;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.Getter;
import org.bithon.server.pipeline.common.service.UriNormalizer;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.transformer.ITransformer;
import org.bithon.server.storage.datasource.input.transformer.TransformResult;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/8/9 12:36
 */
@JsonTypeName("normalize")
public class UriNormalizationTransformer implements ITransformer {

    @Getter
    private final String field;
    private final UriNormalizer normalizer;

    @JsonCreator
    public UriNormalizationTransformer(@JsonProperty("field") String field,
                                       @JacksonInject(useInput = OptBoolean.FALSE) UriNormalizer normalizer) {
        this.field = field == null ? "uri" : field;
        this.normalizer = normalizer;
    }

    @Override
    public TransformResult transform(IInputRow row) {
        UriNormalizer.NormalizedResult result = normalizer.normalize(row.getColAsString("appName"),
                                                                     row.getColAsString(this.field));
        if (result.getUri() == null) {
            throw new TransformException();
        }
        row.updateColumn(field, result.getUri());

        return TransformResult.CONTINUE;
    }
}
