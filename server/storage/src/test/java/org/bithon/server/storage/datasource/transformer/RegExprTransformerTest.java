package org.bithon.server.storage.datasource.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.bithon.server.storage.datasource.input.InputRow;
import org.bithon.server.storage.datasource.input.transformer.ITransformer;
import org.bithon.server.storage.datasource.input.transformer.RegExprTransformer;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

/**
 * @author Frank Chen
 * @date 5/8/22 11:50 AM
 */
public class RegExprTransformerTest {

    @Test
    public void test() throws JsonProcessingException {
        RegExprTransformer transformer = new RegExprTransformer("exception", "Code: ([0-9]+)", "exceptionCode");

        // deserialize from json to test deserialization
        ObjectMapper om = new ObjectMapper();
        String transformerText = om.writeValueAsString(transformer);
        ITransformer newTransformer = om.readValue(transformerText, ITransformer.class);

        InputRow row1 = new InputRow(new HashMap<>(ImmutableMap.of("exception", "Code: 60, e.displayText()")));
        newTransformer.transform(row1);
        Assert.assertEquals("60", row1.getCol("exceptionCode"));
    }

    @Test
    public void testRegExprOnNested() throws JsonProcessingException {
        RegExprTransformer transformer = new RegExprTransformer("tags.exception", "Code: ([0-9]+)", "exceptionCode");

        // deserialize from json to test deserialization
        ObjectMapper om = new ObjectMapper();
        String transformerText = om.writeValueAsString(transformer);
        ITransformer newTransformer = om.readValue(transformerText, ITransformer.class);

        InputRow row1 = new InputRow(new HashMap<>());
        row1.updateColumn("tags", ImmutableMap.of("exception", "Code: 60, e.displayText()"));
        newTransformer.transform(row1);
        Assert.assertEquals("60", row1.getCol("exceptionCode"));
    }
}
