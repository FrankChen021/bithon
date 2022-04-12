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

package org.bithon.server.storage.datasource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import org.bithon.server.storage.datasource.filter.IInputRowFilter;
import org.bithon.server.storage.datasource.flatten.IFlattener;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.transformer.ITransformer;

import java.util.List;

/**
 * @author Frank Chen
 * @date 11/4/22 10:49 PM
 */
@Getter
@Builder
public class TransformSpec {
    private final List<IInputRowFilter> prefilters;
    private final List<IFlattener> flatteners;
    private final List<ITransformer> transformers;
    private final List<IInputRowFilter> postfilters;

    @JsonCreator
    public TransformSpec(@JsonProperty("prefilters") List<IInputRowFilter> prefilters,
                         @JsonProperty("flatteners") List<IFlattener> flatteners,
                         @JsonProperty("transformers") List<ITransformer> transformers,
                         @JsonProperty("postfilters") List<IInputRowFilter> postfilters) {
        this.flatteners = flatteners;
        this.prefilters = prefilters;
        this.transformers = transformers;
        this.postfilters = postfilters;
    }

    /**
     * @return a boolean value, whether to include this row in result set
     */
    public boolean transform(IInputRow inputRow) {
        if (prefilters != null) {
            for (IInputRowFilter filter : prefilters) {
                if (!filter.shouldInclude(inputRow)) {
                    return false;
                }
            }
        }
        if (flatteners != null) {
            for (IFlattener flattener : flatteners) {
                flattener.flatten(inputRow);
            }
        }
        if (transformers != null) {
            for (ITransformer transformer : transformers) {
                transformer.transform(inputRow);
            }
        }
        if (postfilters != null) {
            for (IInputRowFilter filter : postfilters) {
                if (!filter.shouldInclude(inputRow)) {
                    return false;
                }
            }
        }
        return true;
    }
}
