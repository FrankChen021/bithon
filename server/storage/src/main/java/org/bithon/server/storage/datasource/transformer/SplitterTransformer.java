package org.bithon.server.storage.datasource.transformer;

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
                               @JsonProperty("names") String[] names) {
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
