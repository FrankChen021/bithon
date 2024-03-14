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

package org.bithon.server.storage.datasource.input;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.commons.time.Period;
import org.bithon.server.storage.datasource.input.filter.AndFilter;
import org.bithon.server.storage.datasource.input.filter.IInputRowFilter;
import org.bithon.server.storage.datasource.input.flatten.IFlattener;
import org.bithon.server.storage.datasource.input.transformer.AbstractTransformer;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 11/4/22 10:49 PM
 */
@Slf4j
@Builder
@AllArgsConstructor
public class TransformSpec {
    /**
     * the granularity that the data should be aggregated at
     */
    @Getter
    private final Period granularity;

    @Getter
    private final IInputRowFilter prefilter;

    @Getter
    private final List<IFlattener> flatteners;

    @Getter
    private final List<AbstractTransformer> transformers;

    @Getter
    private final IInputRowFilter postfilter;

    @JsonCreator
    public TransformSpec(@JsonProperty("granularity") @Nullable Period granularity,
                         //a backward compatibility filter
                         @JsonProperty("prefilters") List<IInputRowFilter> prefilters,
                         @JsonProperty("prefilter") IInputRowFilter prefilter,
                         @JsonProperty("flatteners") List<IFlattener> flatteners,
                         @JsonProperty("transformers") List<AbstractTransformer> transformers,
                         @JsonProperty("postfilter") IInputRowFilter postfilter) {
        this.granularity = granularity;
        this.flatteners = flatteners;
        this.prefilter = prefilters != null ? new AndFilter(prefilters) : prefilter;
        this.transformers = transformers;
        this.postfilter = postfilter;
    }

    /**
     * @return a boolean value, whether to include this row in result set
     */
    public boolean transform(IInputRow inputRow) {
        try {
            if (prefilter != null) {
                if (!prefilter.shouldInclude(inputRow)) {
                    return false;
                }
            }
            if (flatteners != null) {
                for (IFlattener flattener : flatteners) {
                    flattener.flatten(inputRow);
                }
            }
            if (transformers != null) {
                for (AbstractTransformer transformer : transformers) {
                    try {
                        transformer.transform(inputRow);
                    } catch (AbstractTransformer.TransformException ignored) {
                        return false;
                    }
                }
            }
            if (postfilter != null) {
                return postfilter.shouldInclude(inputRow);
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to transform input data", e);
            return false;
        }
    }
}
