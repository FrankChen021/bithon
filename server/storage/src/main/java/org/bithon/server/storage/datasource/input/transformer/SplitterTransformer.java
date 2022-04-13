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

package org.bithon.server.storage.datasource.input.transformer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.bithon.server.storage.datasource.input.IInputRow;

/**
 * @author Frank Chen
 * @date 11/4/22 11:52 PM
 */
public class SplitterTransformer implements ITransformer {

    @Getter
    private final String source;

    @Getter
    private final String splitter;

    @Getter
    private final String[] names;

    @JsonCreator
    public SplitterTransformer(@JsonProperty("source") String source,
                               @JsonProperty("splitter") String splitter,
                               @JsonProperty("names") String... names) {
        this.splitter = splitter;
        this.names = names;
        this.source = source;
    }

    @Override
    public void transform(IInputRow row) {
        String val = row.getColAsString(source);
        if (val != null) {
            String[] values = val.split(splitter);
            for (int i = 0, len = Math.min(names.length, values.length); i < len; i++) {
                row.updateColumn(names[i], values[i]);
            }
        }
    }
}
