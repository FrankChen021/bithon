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
 * Base implementation of ILogicalPlanVisitor that provides default behavior
 * and utility methods for common visitor operations.
 * <p>
 * This class can be extended to implement specific visitor functionality
 * without having to implement all visitor methods from scratch.
 * 
 * @param <T> the return type of the visitor methods
 * @author frank.chen021@outlook.com
 * @date 2025/6/4 23:31
 */
public abstract class BaseLogicalPlanVisitor<T> implements ILogicalPlanVisitor<T> {

    /**
     * Default implementation for visiting table scans.
     * Can be overridden by subclasses to provide specific behavior.
     */
    @Override
    public T visitTableScan(LogicalTableScan tableScan) {
        return defaultVisit(tableScan);
    }

    /**
     * Default implementation for visiting aggregates.
     * Recursively visit the input plan.
     * It Can be overridden by subclasses to provide specific behavior.
     */
    @Override
    public T visitAggregate(LogicalAggregate aggregate) {
        aggregate.input().accept(this);
        return defaultVisit(aggregate);
    }

    /**
     * Default implementation for visiting binary operations.
     * Recursively visits both left and right operands.
     * It Can be overridden by subclasses to provide specific behavior.
     */
    @Override
    public T visitBinaryOp(LogicalBinaryOp binaryOp) {
        binaryOp.left().accept(this);
        binaryOp.right().accept(this);
        return defaultVisit(binaryOp);
    }

    /**
     * Default implementation for visiting scalars.
     * Can be overridden by subclasses to provide specific behavior.
     */
    @Override
    public T visitScalar(LogicalScalar scalar) {
        return defaultVisit(scalar);
    }

    /**
     * Default implementation for visiting filters.
     * Recursively visits both left and right operands.
     * It Can be overridden by subclasses to provide specific behavior.
     */
    @Override
    public T visitFilter(LogicalFilter filter) {
        filter.left().accept(this);
        filter.right().accept(this);
        return defaultVisit(filter);
    }

    /**
     * Default behavior for all visit methods.
     * Subclasses can override this to provide common default behavior,
     * or override individual visit methods for specific behavior.
     * 
     * @param plan the logical plan being visited
     * @return the default result
     */
    protected abstract T defaultVisit(ILogicalPlan plan);
}
