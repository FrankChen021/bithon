package org.bithon.server.web.service.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Frank Chen
 * @date 29/10/22 10:16 pm
 */
public class GeneralQueryRequestTest {

    @Test
    public void testJSON() throws JsonProcessingException {
        final String json = "{"
                            + "  \"columns\": [\n"
                            + "    \"appName\",\n"
                            + "    {\"name\": \"instanceUpTime\", \"formatter\": \"compact_number\" },\n"
                            + "    {\"name\": \"errorCount\", \"type\": \"expression\", \"expression\": \"errorCount/totalCount*100.0\",\"formatter\": \"compact_number\"}"
                            + "  ],\n"
                            + "  \"orderBy\": {\n"
                            + "    \"name\": \"instanceUpTime\",\n"
                            + "    \"order\": \"desc\"\n"
                            + "  }"
                            + "}";

        GeneralQueryRequest request = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                                                        .readValue(json, GeneralQueryRequest.class);
        Assert.assertEquals(GeneralQueryRequest.QueryColumn.class, request.getColumns().get(0).getClass());
        Assert.assertEquals(GeneralQueryRequest.QueryColumn.class, request.getColumns().get(1).getClass());
        Assert.assertEquals(GeneralQueryRequest.ExpressionQueryColumn.class, request.getColumns().get(2).getClass());

        Assert.assertEquals("instanceUpTime", request.getOrderBy().getName());
        Assert.assertEquals("desc", request.getOrderBy().getOrder());
    }
}
