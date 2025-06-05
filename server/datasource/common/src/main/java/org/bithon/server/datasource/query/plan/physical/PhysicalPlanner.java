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

package org.bithon.server.datasource.query.plan.physical;


import org.bithon.server.datasource.query.plan.logical.ILogicalPlanVisitor;
import org.bithon.server.datasource.query.plan.logical.LogicalAggregate;
import org.bithon.server.datasource.query.plan.logical.LogicalBinaryOp;
import org.bithon.server.datasource.query.plan.logical.LogicalFilter;
import org.bithon.server.datasource.query.plan.logical.LogicalScalar;
import org.bithon.server.datasource.query.plan.logical.LogicalTableScan;

/**
 * @author frank.chen021@outlook.com
 * @date 4/4/25 3:53 pm
 */
public class PhysicalPlanner implements ILogicalPlanVisitor<IPhysicalPlan> {


    @Override
    public IPhysicalPlan visitTableScan(LogicalTableScan tableScan) {
        return null;
    }

    @Override
    public IPhysicalPlan visitAggregate(LogicalAggregate aggregate) {
        return null;
    }

    @Override
    public IPhysicalPlan visitBinaryOp(LogicalBinaryOp binaryOp) {
        // Implementation for visiting binary operations
        return null; // Placeholder
    }

    @Override
    public IPhysicalPlan visitFilter(LogicalFilter filter) {
        // Implementation for visiting filters
        return null; // Placeholder
    }

    @Override
    public IPhysicalPlan visitScalar(LogicalScalar scalar) {
        // Implementation for visiting scalars
        return null; // Placeholder
    }
}
