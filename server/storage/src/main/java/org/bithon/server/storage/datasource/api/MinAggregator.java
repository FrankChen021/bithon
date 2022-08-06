package org.bithon.server.storage.datasource.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.bithon.component.commons.utils.Preconditions;

import javax.validation.constraints.NotNull;

/**
 * @author Frank Chen
 * @date 5/8/22 4:59 PM
 */
public class MinAggregator implements IQueryableAggregator {

    public static final String TYPE = "min";
    @Getter
    private final String name;

    @Getter
    private final String field;

    @JsonCreator
    public MinAggregator(@JsonProperty("name") @NotNull String name,
                         @JsonProperty("field") @NotNull String field) {
        this.name = Preconditions.checkArgumentNotNull("name", name);
        this.field = Preconditions.checkArgumentNotNull("field", field);
    }

    @Override
    public <T> T accept(IQueryableAggregatorVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
