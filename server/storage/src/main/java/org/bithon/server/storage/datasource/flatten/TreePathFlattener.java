package org.bithon.server.storage.datasource.flatten;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.bithon.server.storage.datasource.input.IInputRow;

import java.util.Map;

/**
 * @author Frank Chen
 * @date 11/4/22 11:17 PM
 */
public class TreePathFlattener implements IFlattener {

    @Getter
    private final String name;

    @Getter
    private final String[] nodes;

    @JsonCreator
    public TreePathFlattener(@JsonProperty("name") String name,
                             @JsonProperty("nodes") String[] nodes) {
        this.name = name;
        this.nodes = nodes;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void flatten(IInputRow inputRow) {
        Object obj = inputRow.getCol(nodes[0]);
        if (!(obj instanceof Map)) {
            // error
            return;
        }

        Map map = (Map) obj;
        for (int i = 1; i < nodes.length - 1; i++) {
            obj = map.get(nodes[i]);
            if (!(obj instanceof Map)) {
                return;
            }
            map = (Map) obj;
        }

        Object val = map.get(nodes[nodes.length - 1]);
        if (val != null) {
            inputRow.updateColumn(name, val);
        }
    }
}
