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
 * Visitor interface for the ILogicalPlan hierarchy.
 * Implements the visitor pattern to allow operations on logical plans 
 * without modifying the plan classes themselves.
 * 
 * @param <T> the return type of the visitor methods
 * @author frank.chen021@outlook.com
 * @date 2025/6/4 23:31
 */
public interface ILogicalPlanVisitor<T> {

    /**
     * Visit a LogicalTableScan node
     * @param tableScan the table to visit
     * @return the result of visiting the table scan
     */
    T visitTableScan(LogicalTableScan tableScan);

    /**
     * Visit a LogicalAggregate node
     * @param aggregate the aggregate to visit
     * @return the result of visiting the aggregate
     */
    T visitAggregate(LogicalAggregate aggregate);

    /**
     * Visit a LogicalBinaryOp node
     * @param binaryOp the binary operation to visit
     * @return the result of visiting the binary operation
     */
    T visitBinaryOp(LogicalBinaryOp binaryOp);

    /**
     * Visit a LogicalScalar node
     * @param scalar the scalar to visit
     * @return the result of visiting the scalar
     */
    T visitScalar(LogicalScalar scalar);

    /**
     * Visit a LogicalFilter node
     * @param filter the filter to visit
     * @return the result of visiting the filter
     */
    T visitFilter(LogicalFilter filter);
}
