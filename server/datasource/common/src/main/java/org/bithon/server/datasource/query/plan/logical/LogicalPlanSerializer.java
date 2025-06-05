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

package org.bithon.server.datasource.query.plan.logical;

/**
 * Concrete visitor implementation that converts logical plans to string representation.
 * This serves as an example of how to implement and use the visitor pattern.
 * 
 * @author frank.chen021@outlook.com
 * @date 2025/6/4 23:31
 */
public class LogicalPlanSerializer implements ILogicalPlanVisitor<String> {

    private int indentLevel = 0;
    private static final String INDENT = "  ";

    @Override
    public String visitTableScan(LogicalTableScan tableScan) {
        return indent() + "TableScan(" +
               "table='" + tableScan.table() + "', " +
               "filters=" + (tableScan.filter() != null ? tableScan.filter().toString() : "null") +
               ")";
    }

    @Override
    public String visitAggregate(LogicalAggregate aggregate) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append("Aggregate(");
        sb.append("func='").append(aggregate.func()).append("', ");
        sb.append("groupBy=").append(aggregate.groupBy()).append(",\n");
        
        indentLevel++;
        String inputStr = aggregate.input().accept(this);
        indentLevel--;
        
        sb.append(inputStr).append("\n");
        sb.append(indent()).append(")");
        return sb.toString();
    }

    @Override
    public String visitBinaryOp(LogicalBinaryOp binaryOp) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append("BinaryOp(op=").append(binaryOp.op()).append(",\n");
        
        indentLevel++;
        String leftStr = binaryOp.left().accept(this);
        String rightStr = binaryOp.right().accept(this);
        indentLevel--;
        
        sb.append(leftStr).append(",\n");
        sb.append(rightStr).append("\n");
        sb.append(indent()).append(")");
        return sb.toString();
    }

    @Override
    public String visitScalar(LogicalScalar scalar) {
        return indent() + "Scalar(value=" + scalar.value() + ")";
    }

    @Override
    public String visitFilter(LogicalFilter filter) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append("Filter(op=").append(filter.op()).append(",\n");
        
        indentLevel++;
        String leftStr = filter.left().accept(this);
        String rightStr = filter.right().accept(this);
        indentLevel--;
        
        sb.append(leftStr).append(",\n");
        sb.append(rightStr).append("\n");
        sb.append(indent()).append(")");
        return sb.toString();
    }

    private String indent() {
        return INDENT.repeat(indentLevel);
    }

    /**
     * Utility method to convert any logical plan to string representation
     */
    public static String toString(ILogicalPlan plan) {
        return plan.accept(new LogicalPlanSerializer());
    }
}
