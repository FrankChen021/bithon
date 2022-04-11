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
 * @date 12/4/22 12:06 AM
 */
public class SplitterTransfomerTest {

    @Test
    public void test() throws JsonProcessingException {
        SplitterTransformer transformer = new SplitterTransformer("o1", "\\.", new String[]{"database", "table"});

        // deserialize from json to test deserialization
        ObjectMapper om = new ObjectMapper();
        String transformerText = om.writeValueAsString(transformer);
        ITransformer newTransformer = om.readValue(transformerText, ITransformer.class);

        InputRow row1 = new InputRow(new HashMap<>(ImmutableMap.of("o1", "default.user")));
        newTransformer.transform(row1);
        Assert.assertEquals("default", row1.getCol("database"));
        Assert.assertEquals("user", row1.getCol("table"));

        // field not match
        InputRow row2 = new InputRow(new HashMap<>(ImmutableMap.of("o2", "default.user")));
        newTransformer.transform(row2);
        Assert.assertNull(row2.getCol("database"));
        Assert.assertNull(row2.getCol("table"));
    }
}
