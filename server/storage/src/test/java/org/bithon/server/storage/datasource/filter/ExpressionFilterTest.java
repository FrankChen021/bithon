package org.bithon.server.storage.datasource.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.InputRow;
import org.bithon.server.storage.datasource.input.filter.ExpressionFilter;
import org.bithon.server.storage.datasource.input.filter.IInputRowFilter;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

/**
 * @author Frank Chen
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
    public void testCompoundExpressioOR() {
        ExpressionFilter filter = new ExpressionFilter("a = b OR c = d");

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
}
