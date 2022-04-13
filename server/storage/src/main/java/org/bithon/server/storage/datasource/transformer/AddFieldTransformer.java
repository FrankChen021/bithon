package org.bithon.server.storage.datasource.transformer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.bithon.server.storage.datasource.input.IInputRow;

/**
 * @author Frank Chen
 * @date 13/4/22 4:57 PM
 */
public class AddFieldTransformer implements ITransformer {

    @Getter
    private final String name;

    @Getter
    private final String value;

    @JsonCreator
    public AddFieldTransformer(@JsonProperty("name") String name,
                               @JsonProperty("value") String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public void transform(IInputRow inputRow) {
        if ( name != null && value != null) {
            inputRow.updateColumn(name, value);
        }
    }
}
