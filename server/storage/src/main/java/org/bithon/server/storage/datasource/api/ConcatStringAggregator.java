package org.bithon.server.storage.datasource.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.bithon.component.commons.utils.Preconditions;

import javax.validation.constraints.NotNull;

/**
 * @author Frank Chen
 * @date 5/8/22 4:56 PM
 */
public class ConcatStringAggregator implements IQueryableAggregator {

    public static final String TYPE = "concatString";
    @Getter
    private final String name;

    @Getter
    private final String dimension;

    @JsonCreator
    public ConcatStringAggregator(@JsonProperty("name") @NotNull String name,
                                  @JsonProperty("dimension") @NotNull String dimension) {
        this.name = Preconditions.checkArgumentNotNull("name", name);
        this.dimension = Preconditions.checkArgumentNotNull("dimension", dimension);
    }

    @Override
    public <T> T accept(IQueryableAggregatorVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
