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

package org.bithon.server.storage.datasource.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.InputRow;
import org.bithon.server.storage.datasource.input.filter.ExpressionFilter;
import org.bithon.server.storage.datasource.input.filter.IInputRowFilter;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

/**
 * @author frank.chen021@outlook.com
 * @date 4/8/22 5:13 PM
 */
public class ExpressionFilterTest {

    @Test
    public void testBinaryExpressionEQ() {
        ExpressionFilter filter = new ExpressionFilter("a = b");

        IInputRow row = new InputRow(new HashMap<>());
        row.updateColumn("a", "1");
        row.updateColumn("b", "1");
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", 1);
        row.updateColumn("b", 1);
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", "1");
        row.updateColumn("b", 1);
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", 1);
        row.updateColumn("b", "1");
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", 1);
        row.updateColumn("b", 0);
        Assert.assertFalse(filter.shouldInclude(row));

        row.updateColumn("a", 27);
        row.updateColumn("b", "35");
        Assert.assertFalse(filter.shouldInclude(row));

        row.updateColumn("a", "35");
        row.updateColumn("b", 27);
        Assert.assertFalse(filter.shouldInclude(row));

        row.updateColumn("a", null);
        row.updateColumn("b", 27);
        Assert.assertFalse(filter.shouldInclude(row));

        row.updateColumn("a", null);
        row.updateColumn("b", null);
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", 27);
        row.updateColumn("b", null);
        Assert.assertFalse(filter.shouldInclude(row));
    }

    @Test
    public void testBinaryExpressionNE() {
        ExpressionFilter filter = new ExpressionFilter("a != b");

        IInputRow row = new InputRow(new HashMap<>());
        row.updateColumn("a", "1");
        row.updateColumn("b", "1");
        Assert.assertFalse(filter.shouldInclude(row));

        row.updateColumn("a", 1);
        row.updateColumn("b", 1);
        Assert.assertFalse(filter.shouldInclude(row));

        row.updateColumn("a", "1");
        row.updateColumn("b", 1);
        Assert.assertFalse(filter.shouldInclude(row));

        row.updateColumn("a", 1);
        row.updateColumn("b", "1");
        Assert.assertFalse(filter.shouldInclude(row));

        row.updateColumn("a", 1);
        row.updateColumn("b", 0);
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", 27);
        row.updateColumn("b", "35");
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", "35");
        row.updateColumn("b", 27);
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", null);
        row.updateColumn("b", 27);
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", null);
        row.updateColumn("b", null);
        Assert.assertFalse(filter.shouldInclude(row));

        row.updateColumn("a", 27);
        row.updateColumn("b", null);
        Assert.assertTrue(filter.shouldInclude(row));
    }

    @Test
    public void testCompoundExpressionAND() {
        ExpressionFilter filter = new ExpressionFilter("a = b and c = d");

        IInputRow row = new InputRow(new HashMap<>());

        row.updateColumn("a", "1");
        row.updateColumn("b", "1");
        row.updateColumn("c", "2");
        row.updateColumn("d", "2");
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", 1);
        row.updateColumn("b", 1);
        row.updateColumn("c", 2);
        row.updateColumn("d", 2);
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", 27);
        row.updateColumn("b", 1);
        row.updateColumn("c", 2);
        row.updateColumn("d", 2);
        Assert.assertFalse(filter.shouldInclude(row));

        row.updateColumn("a", 27);
        row.updateColumn("b", 27);
        row.updateColumn("c", 36);
        row.updateColumn("d", 2);
        Assert.assertFalse(filter.shouldInclude(row));
    }

    @Test
    public void testCompoundExpressionOR() {
        ExpressionFilter filter = new ExpressionFilter("a = b OR c = d", true);

        IInputRow row = new InputRow(new HashMap<>());

        row.updateColumn("a", "1");
        row.updateColumn("b", "1");
        row.updateColumn("c", "2");
        row.updateColumn("d", "3");
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", 1);
        row.updateColumn("b", 1);
        row.updateColumn("c", 2);
        row.updateColumn("d", 3);
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", 27);
        row.updateColumn("b", 1);
        row.updateColumn("c", 2);
        row.updateColumn("d", 2);
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", 27);
        row.updateColumn("b", 1);
        row.updateColumn("c", "2");
        row.updateColumn("d", "2");
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", 27);
        row.updateColumn("b", 1);
        row.updateColumn("c", 5);
        row.updateColumn("d", 6);
        Assert.assertFalse(filter.shouldInclude(row));
    }

    @Test
    public void testCompoundExpressionWithBrace() {
        ExpressionFilter filter = new ExpressionFilter("(a = b)");

        IInputRow row = new InputRow(new HashMap<>());

        row.updateColumn("a", "1");
        row.updateColumn("b", "1");
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", 1);
        row.updateColumn("b", 1);
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", 27);
        row.updateColumn("b", 1);
        Assert.assertFalse(filter.shouldInclude(row));
    }


    @Test
    public void testCompoundExpressionWithBraceLevel2() {
        ExpressionFilter filter = new ExpressionFilter("((a = b))");

        IInputRow row = new InputRow(new HashMap<>());

        row.updateColumn("a", "1");
        row.updateColumn("b", "1");
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", 1);
        row.updateColumn("b", 1);
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", 27);
        row.updateColumn("b", 1);
        Assert.assertFalse(filter.shouldInclude(row));
    }

    @Test
    public void testCompoundExpressionsWithBrace() {
        ExpressionFilter filter = new ExpressionFilter("((a = b) or (c = d)) AND (e = f)");

        IInputRow row = new InputRow(new HashMap<>());

        row.updateColumn("a", "1");
        row.updateColumn("b", "1");
        row.updateColumn("e", "1");
        row.updateColumn("f", "1");
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", "1");
        row.updateColumn("b", "12");
        row.updateColumn("c", "57");
        row.updateColumn("d", "58");
        row.updateColumn("e", "1");
        row.updateColumn("f", "1");
        Assert.assertFalse(filter.shouldInclude(row));

        row.updateColumn("a", "1");
        row.updateColumn("b", "1");
        row.updateColumn("e", "1");
        row.updateColumn("f", "13");
        Assert.assertFalse(filter.shouldInclude(row));

        row.updateColumn("a", "1");
        row.updateColumn("b", "12");
        row.updateColumn("c", "35");
        row.updateColumn("d", "35");
        row.updateColumn("e", "1");
        row.updateColumn("f", "1");
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", "1");
        row.updateColumn("b", "12");
        row.updateColumn("c", "35");
        row.updateColumn("d", "37");
        row.updateColumn("e", "1");
        row.updateColumn("f", "1");
        Assert.assertFalse(filter.shouldInclude(row));
    }

    @Test
    public void testNumericConstant() {
        ExpressionFilter filter = new ExpressionFilter("a = 5");

        IInputRow row = new InputRow(new HashMap<>());

        row.updateColumn("a", 5);
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", "5");
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", "6");
        Assert.assertFalse(filter.shouldInclude(row));

        // empty row
        Assert.assertFalse(filter.shouldInclude(new InputRow(new HashMap<>())));
    }

    @Test
    public void testStringConstant() {
        ExpressionFilter filter = new ExpressionFilter("a = '5'");

        IInputRow row = new InputRow(new HashMap<>());

        row.updateColumn("a", 5);
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", "5");
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", "6");
        Assert.assertFalse(filter.shouldInclude(row));

        // empty row
        Assert.assertFalse(filter.shouldInclude(new InputRow(new HashMap<>())));
    }

    @Test
    public void testEmptyString() {
        ExpressionFilter filter = new ExpressionFilter("a = ''");

        IInputRow row = new InputRow(new HashMap<>());

        row.updateColumn("a", "");
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", "6");
        Assert.assertFalse(filter.shouldInclude(row));

        // empty row
        Assert.assertFalse(filter.shouldInclude(new InputRow(new HashMap<>())));
    }

    @Test
    public void testExpressionDeserialization() throws JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        String json = om.writeValueAsString(new ExpressionFilter("a = '5'"));
        IInputRowFilter filter = om.readValue(json, IInputRowFilter.class);

        IInputRow row = new InputRow(new HashMap<>());

        row.updateColumn("a", 5);
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", "5");
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", "6");
        Assert.assertFalse(filter.shouldInclude(row));

        // empty row
        Assert.assertFalse(filter.shouldInclude(new InputRow(new HashMap<>())));
    }

    @Test
    public void testOperator_GT() throws JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        String json = om.writeValueAsString(new ExpressionFilter("a > 5"));
        IInputRowFilter filter = om.readValue(json, IInputRowFilter.class);

        IInputRow row = new InputRow(new HashMap<>());

        row.updateColumn("a", 5);
        Assert.assertFalse(filter.shouldInclude(row));

        row.updateColumn("a", 6);
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", 4);
        Assert.assertFalse(filter.shouldInclude(row));

        // empty row
        Assert.assertFalse(filter.shouldInclude(new InputRow(new HashMap<>())));
    }

    @Test
    public void testQualifiedName() throws JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        String json = om.writeValueAsString(new ExpressionFilter("a.b.c > 5", true));
        IInputRowFilter filter = om.readValue(json, IInputRowFilter.class);

        IInputRow row = new InputRow(new HashMap<>());

        row.updateColumn("a.b.c", 5);
        Assert.assertFalse(filter.shouldInclude(row));

        row.updateColumn("a.b.c", 6);
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a.b.c", 4);
        Assert.assertFalse(filter.shouldInclude(row));

        // empty row
        Assert.assertFalse(filter.shouldInclude(new InputRow(new HashMap<>())));
    }

    @Test
    public void testOperator_IN() throws JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        String json = om.writeValueAsString(new ExpressionFilter("a in (5,6,7)", true));
        IInputRowFilter filter = om.readValue(json, IInputRowFilter.class);

        IInputRow row = new InputRow(new HashMap<>());

        row.updateColumn("a", 0);
        Assert.assertFalse(filter.shouldInclude(row));

        row.updateColumn("a", 5);
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", 6);
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", 7);
        Assert.assertTrue(filter.shouldInclude(row));

        row.updateColumn("a", 8);
        Assert.assertFalse(filter.shouldInclude(row));

        // empty row
        Assert.assertFalse(filter.shouldInclude(new InputRow(new HashMap<>())));
    }

    @Test
    public void testOperator_LIKE() throws JsonProcessingException {
        String[] operators = new String[]{"like", "LIKE", "lIke"};
        for (String operator : operators) {
            ObjectMapper om = new ObjectMapper();
            String json = om.writeValueAsString(new ExpressionFilter(StringUtils.format("a %s 'b'", operator), true));
            IInputRowFilter filter = om.readValue(json, IInputRowFilter.class);

            IInputRow row = new InputRow(new HashMap<>());

            // a does NOT like c
            row.updateColumn("a", "c");
            Assert.assertFalse(filter.shouldInclude(row));

            // a does LIKE 'b'
            row.updateColumn("a", "b");
            Assert.assertTrue(filter.shouldInclude(row));

            // empty row
            Assert.assertFalse(filter.shouldInclude(new InputRow(new HashMap<>())));
        }
    }
}
