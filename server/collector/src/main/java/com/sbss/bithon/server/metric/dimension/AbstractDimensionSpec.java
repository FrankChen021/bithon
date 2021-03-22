package com.sbss.bithon.server.metric.dimension;

import com.sbss.bithon.server.metric.dimension.transformer.IDimensionTransformer;
import lombok.Data;
import lombok.Getter;

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
