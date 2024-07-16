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

package org.bithon.server.pipeline.common.transform;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.logging.RateLimitLogger;
import org.bithon.server.commons.time.Period;
import org.bithon.server.pipeline.common.transform.transformer.ITransformer;
import org.bithon.server.pipeline.common.transform.transformer.TransformResult;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 11/4/22 10:49 PM
 */
@Slf4j
@Builder
public class TransformSpec {
    private static final Logger LOG = new RateLimitLogger(log).config(Level.ERROR, 1);

    /**
     * the granularity that the data should be aggregated at
     */
    @Getter
    private final Period granularity;

    @Getter
    private final List<ITransformer> transformers;

    @JsonCreator
    public TransformSpec(@JsonProperty("granularity") @Nullable Period granularity,
                         @JsonProperty("transformers") List<ITransformer> transformers) {
        this.granularity = granularity;
        this.transformers = Preconditions.checkArgumentNotNull("transformers", transformers);
    }

    /**
     * @return a boolean value, whether to include this row in result set.
     * If true, the row will be included
     */
    public boolean transform(IInputRow inputRow) {
        try {
            for (ITransformer transformer : transformers) {
                try {
                    if (transformer.transform(inputRow) == TransformResult.DROP) {
                        return false;
                    }
                } catch (ITransformer.TransformException e) {
                    LOG.error(e.getMessage());
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            LOG.error(StringUtils.format("Failed to transform input data: %s", inputRow), e);
            return false;
        }
    }
}
