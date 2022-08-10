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

package org.bithon.server.sink.metrics.transformer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import org.bithon.server.sink.common.utils.NetworkUtils;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.transformer.ITransformer;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/8/10 20:45
 */
@JsonTypeName("extractPath")
public class ExtractPath implements ITransformer {

    @Getter
    private final String uri;

    @Getter
    private final String targetField;

    @JsonCreator
    public ExtractPath(@JsonProperty("uri") String uri,
                       @JsonProperty("targetField") String targetField) {
        this.uri = uri;
        this.targetField = targetField;
    }

    @Override
    public void transform(IInputRow inputRow) throws TransformException {
        try {
            URI uri = new URI(inputRow.getColAsString(this.uri));
            inputRow.updateColumn(this.targetField, NetworkUtils.formatUri(uri));
        } catch (URISyntaxException ignored) {
        }
    }
}
