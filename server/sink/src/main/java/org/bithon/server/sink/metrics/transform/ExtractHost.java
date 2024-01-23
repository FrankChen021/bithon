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

package org.bithon.server.sink.metrics.transform;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.transformer.ITransformer;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/8/9 20:28
 */
@JsonTypeName("extractHost")
public class ExtractHost implements ITransformer {

    @Getter
    private final String uri;

    @Getter
    private final String targetField;

    @JsonCreator
    public ExtractHost(@JsonProperty("uri") String uri,
                       @JsonProperty("targetField") String field) {
        this.uri = uri;
        this.targetField = field;
    }

    @Override
    public boolean transform(IInputRow inputRow) throws TransformException {
        try {
            URI uri = new URI(inputRow.getColAsString(this.uri));
            String hostAndPort = toHostPort(uri.getHost(), uri.getPort());
            if (hostAndPort == null) {
                throw new TransformException();
            }

            inputRow.updateColumn(this.targetField, hostAndPort);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    private String toHostPort(String targetHost, int targetPort) {
        if (StringUtils.isEmpty(targetHost)) {
            return null;
        }

        if (targetPort < 0) {
            return targetHost;
        } else {
            return targetHost + ":" + targetPort;
        }
    }
}
