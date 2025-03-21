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

package org.bithon.server.alerting.common.evaluator.result;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.bithon.server.storage.alerting.Label;

/**
 * @author frank.chen021@outlook.com
 * @date 2020-08-25 15:48:07
 */
@Getter
@Builder
@AllArgsConstructor
public class EvaluationOutput {

    private final boolean matched;
    private final Label label;
    private final String base;
    private final String current;
    private final String delta;
    private final String threshold;

    /**
     * A runtime property
     */
    @Setter
    @JsonIgnore
    private String expressionId;

    @Setter
    @JsonIgnore
    private long start;

    @Setter
    @JsonIgnore
    private long end;

    @JsonCreator
    public EvaluationOutput(@JsonProperty("matched") boolean matched,
                            @JsonProperty("label") Label label,
                            @JsonProperty("base") String base,
                            @JsonProperty("current") String current,
                            @JsonProperty("delta") String delta,
                            @JsonProperty("threshold") String threshold) {
        this.matched = matched;
        this.label = label;
        this.base = base;
        this.current = current;
        this.delta = delta;
        this.threshold = threshold;
    }
}
