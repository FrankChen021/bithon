/*
 *    Copyright 2020 bithon.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.bithon.server.web.service.datasource.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeFieldImpl;
import org.apache.calcite.sql.type.SqlTypeName;
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
                            + "    {\"name\": \"errorCount\", \"expression\": \"errorCount/totalCount*100.0\",\"formatter\": \"compact_number\"}"
                            + "  ],\n"
                            + "  \"orderBy\": {\n"
                            + "    \"name\": \"instanceUpTime\",\n"
                            + "    \"order\": \"desc\"\n"
                            + "  }"
                            + "}";

        GeneralQueryRequest request = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                                                        .readValue(json, GeneralQueryRequest.class);

        Assert.assertEquals("instanceUpTime", request.getOrderBy().getName());
        Assert.assertEquals("desc", request.getOrderBy().getOrder());
    }

    @Test
    public void testStringFormatLimit() throws JsonProcessingException {
        final String json = "{"
                            + "  \"columns\": [\n"
                            + "    \"appName\",\n"
                            + "    {\"name\": \"instanceUpTime\", \"formatter\": \"compact_number\" },\n"
                            + "    {\"name\": \"errorCount\", \"expression\": \"errorCount/totalCount*100.0\",\"formatter\": \"compact_number\"}"
                            + "  ],\n"
                            + "  \"orderBy\": {\n"
                            + "    \"name\": \"instanceUpTime\",\n"
                            + "    \"order\": \"desc\"\n"
                            + "  },"
                            + "  \"limit\": \"15\""
                            + "}";

        GeneralQueryRequest request = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                                                        .readValue(json, GeneralQueryRequest.class);

        Assert.assertEquals("instanceUpTime", request.getOrderBy().getName());
        Assert.assertEquals("desc", request.getOrderBy().getOrder());
        Assert.assertEquals(15, request.getLimit().getLimit());
    }

    @Test
    public void testNumberFormatLimit() throws JsonProcessingException {
        final String json = "{"
                            + "  \"columns\": [\n"
                            + "    \"appName\",\n"
                            + "    {\"name\": \"instanceUpTime\", \"formatter\": \"compact_number\" },\n"
                            + "    {\"name\": \"errorCount\", \"expression\": \"errorCount/totalCount*100.0\",\"formatter\": \"compact_number\"}"
                            + "  ],\n"
                            + "  \"orderBy\": {\n"
                            + "    \"name\": \"instanceUpTime\",\n"
                            + "    \"order\": \"desc\"\n"
                            + "  },"
                            + "  \"limit\": 15"
                            + "}";

        GeneralQueryRequest request = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                                                        .readValue(json, GeneralQueryRequest.class);

        Assert.assertEquals("instanceUpTime", request.getOrderBy().getName());
        Assert.assertEquals("desc", request.getOrderBy().getOrder());
        Assert.assertEquals(15, request.getLimit().getLimit());
    }

    @Test
    public void testLimitObject() throws JsonProcessingException {
        final String json = "{"
                            + "  \"columns\": [\n"
                            + "    \"appName\",\n"
                            + "    {\"name\": \"instanceUpTime\", \"formatter\": \"compact_number\" },\n"
                            + "    {\"name\": \"errorCount\", \"expression\": \"errorCount/totalCount*100.0\",\"formatter\": \"compact_number\"}"
                            + "  ],\n"
                            + "  \"orderBy\": {\n"
                            + "    \"name\": \"instanceUpTime\",\n"
                            + "    \"order\": \"desc\"\n"
                            + "  },"
                            + "  \"limit\": { \"limit\": 4, \"offset\": 6}"
                            + "}";

        GeneralQueryRequest request = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                                                        .readValue(json, GeneralQueryRequest.class);

        Assert.assertEquals("instanceUpTime", request.getOrderBy().getName());
        Assert.assertEquals("desc", request.getOrderBy().getOrder());
        Assert.assertEquals(4, request.getLimit().getLimit());
        Assert.assertEquals(6, request.getLimit().getOffset());
    }
}
