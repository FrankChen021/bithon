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

package org.bithon.server.metric.dimension;

import lombok.Data;
import lombok.Getter;
import org.bithon.server.metric.dimension.transformer.IDimensionTransformer;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/11 10:11 上午
 */
@Data
public abstract class AbstractDimensionSpec implements IDimensionSpec {

    @Getter
    @NotNull
    private final String name;

    @Getter
    @NotNull
    private final String displayText;

    @Getter
    private final boolean required;

    @Getter
    private final boolean visible;

    @Getter
    private final IDimensionTransformer transformer;

    public AbstractDimensionSpec(@NotNull String name,
                                 @NotNull String displayText,
                                 @Nullable Boolean required,
                                 @Nullable Boolean visible,
                                 @Nullable IDimensionTransformer transformer) {
        this.name = name;
        this.displayText = displayText;
        this.required = required == null ? true : required;
        this.visible = visible == null ? true : visible;
        this.transformer = transformer;
    }
}
