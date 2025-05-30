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

package org.bithon.server.storage.tracing.reader;

import lombok.Getter;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.expression.ArithmeticExpression;
import org.bithon.component.commons.expression.ArrayAccessExpression;
import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.ExpressionList;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionVisitor;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.MacroExpression;
import org.bithon.component.commons.expression.MapAccessExpression;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.datasource.column.IColumn;
import org.bithon.server.datasource.column.ObjectColumn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/11/10 11:48
 */
public class TraceFilterSplitter {
    @Getter
    private IExpression expression;

    @Getter
    private List<IExpression> indexedTagFilters;

    private final ISchema summaryTableSchema;
    private final ISchema indexTableSchema;

    public TraceFilterSplitter(ISchema summaryTableSchema,
                               ISchema indexTableSchema) {
        this.summaryTableSchema = summaryTableSchema;
        this.indexTableSchema = indexTableSchema;
    }

    public void split(IExpression expression) {
        if (expression == null) {
            this.indexedTagFilters = Collections.emptyList();
            return;
        }

        if (!(expression instanceof LogicalExpression)
            && !(expression instanceof ConditionalExpression)
        ) {
            throw new HttpMappableException(400,
                                            "The given expression is neither a logical expression(and/or/not) nor a conditional expression, but a %s",
                                            expression.getType());
        }

        SplitterImpl splitter = new SplitterImpl(this.summaryTableSchema, this.indexTableSchema);
        expression.accept(splitter);
        this.indexedTagFilters = splitter.indexedTagFilters;
        this.expression = expression;
    }

    /**
     * This Splitter extracts filters on the 'attributes' column which is indexed
     * so that the execution engine will generate queries on the indexed table.
     * <p>
     * For example, let's say the tag 'http.method'
     * is indexed to the first column (defined in the {@link org.bithon.server.storage.tracing.index.TagIndexConfig}),
     * When the given expression is as:
     *
     * <pre><code>
     * tags['http.method'] = 'GET' AND http.status = 200
     * </code></pre>
     * <p>
     * After the processing of this class, the expression will be turned into:
     * <pre><code>
     * http.status = 200
     * </code></pre>
     * <p>
     * and the indexedTagFilters field will hold expression as
     * <pre><code>
     * f1 = 'GET'
     * </code></pre>
     */
    private static class SplitterImpl implements IExpressionVisitor<Boolean> {

        private final ISchema indexTableSchema;
        private final ISchema summaryTableSchema;
        private final List<IExpression> indexedTagFilters;

        public SplitterImpl(ISchema summaryTableSchema,
                            ISchema indexTableSchema) {
            this.indexedTagFilters = new ArrayList<>();
            this.summaryTableSchema = summaryTableSchema;
            this.indexTableSchema = indexTableSchema;
        }

        @Override
        public Boolean visit(ExpressionList expression) {
            return false;
        }

        @Override
        public Boolean visit(ArrayAccessExpression expression) {
            return false;
        }

        @Override
        public Boolean visit(ArithmeticExpression expression) {
            return false;
        }

        @Override
        public Boolean visit(MacroExpression expression) {
            return false;
        }

        @Override
        public Boolean visit(FunctionExpression expression) {
            return false;
        }

        @Override
        public Boolean visit(ConditionalExpression expression) {
            IExpression left = expression.getLhs();
            if (!(left instanceof MapAccessExpression mapAccessExpression)) {
                return false;
            }

            IExpression mapContainerExpression = mapAccessExpression.getMap();
            if (!(mapContainerExpression instanceof IdentifierExpression)) {
                throw new UnsupportedOperationException("Only identifier is supported as map container");
            }
            String mapColumnName = ((IdentifierExpression) mapContainerExpression).getIdentifier();

            IColumn mapColumn = this.summaryTableSchema.getColumnByName(mapColumnName);
            if (!(mapColumn instanceof ObjectColumn)) {
                throw new HttpMappableException(400,
                                                "The column [%s] is not a map column",
                                                mapColumnName);
            }
            ((IdentifierExpression) mapContainerExpression).setIdentifier(mapColumn.getName());

            IColumn indexColumn = this.indexTableSchema.getColumnByName(mapAccessExpression.getKey());
            if (indexColumn != null) {
                // Replace the map access expression in the left to identifier expression
                // which refers to the indexed column
                expression.setLhs(new IdentifierExpression(indexColumn.getName()));
                return true;
            }

            return false;
        }

        @Override
        public Boolean visit(LogicalExpression expression) {
            if (!(expression instanceof LogicalExpression.AND)) {
                throw new RuntimeException("Only AND operator is supported to search tracing.");
            }

            List<IExpression> nonTagsFilters = new ArrayList<>();
            for (IExpression subExpression : expression.getOperands()) {
                if (subExpression.accept(this)) {
                    indexedTagFilters.add(subExpression);
                } else {
                    nonTagsFilters.add(subExpression);
                }
            }
            if (nonTagsFilters.isEmpty()) {
                // Add a placeholder expression so simple further processing
                nonTagsFilters.add(new ComparisonExpression.EQ(LiteralExpression.ofLong(1), LiteralExpression.ofLong(1)));
            }
            expression.setOperands(nonTagsFilters);

            return false;
        }
    }
}
