package org.bithon.server.storage.datasource.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.bithon.server.storage.datasource.input.InputRow;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Frank Chen
 * @date 11/4/22 11:48 PM
 */
public class EqualFilterTest {

    @Test
    public void test() throws JsonProcessingException {
        EqualFilter filter = new EqualFilter("f1", 1);

        ObjectMapper om = new ObjectMapper();
        String json = om.writeValueAsString(filter);
        IInputRowFilter newFilter = om.readValue(json, IInputRowFilter.class);

        Assert.assertTrue(newFilter.shouldInclude(new InputRow(ImmutableMap.of("f1", 1))));
        Assert.assertFalse(newFilter.shouldInclude(new InputRow(ImmutableMap.of("f1", 2))));

        // field not exist
        Assert.assertFalse(newFilter.shouldInclude(new InputRow(ImmutableMap.of("f2", 2))));
    }
}
