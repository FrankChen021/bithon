package org.bithon.server.storage.datasource.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.bithon.server.storage.datasource.input.InputRow;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

/**
 * @author Frank Chen
 * @date 11/4/22 11:37 PM
 */
public class MappingTransformerTest {

    /**
     * mapping value of field 'o1'
     */
    @Test
    public void basicMapping() throws JsonProcessingException {
        MappingTransformer transformer = new MappingTransformer("o1",
                                                                ImmutableMap.of("v1", "v1-new",
                                                                                "v2", "v2-new"));

        // deserialize from json to test deserialization
        ObjectMapper om = new ObjectMapper();
        String transformerText = om.writeValueAsString(transformer);
        ITransformer newTransformer = om.readValue(transformerText, ITransformer.class);

        InputRow row1 = new InputRow(new HashMap<>(ImmutableMap.of("o1", "v1")));
        newTransformer.transform(row1);
        Assert.assertEquals("v1-new", row1.getCol("o1"));

        InputRow row2 = new InputRow(new HashMap<>(ImmutableMap.of("o1", "v2")));
        newTransformer.transform(row2);
        Assert.assertEquals("v2-new", row2.getCol("o1"));

        // v3 is not in the map, will map to original value
        InputRow row3 = new InputRow(new HashMap<>(ImmutableMap.of("o1", "v3")));
        newTransformer.transform(row3);
        Assert.assertEquals("v3", row3.getCol("o1"));
    }
}
