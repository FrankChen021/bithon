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

package org.bithon.server.web.service.common.sql;

import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.config.CalciteConnectionConfig;
import org.apache.calcite.config.CalciteConnectionConfigImpl;
import org.apache.calcite.config.CalciteConnectionProperty;
import org.apache.calcite.interpreter.BindableConvention;
import org.apache.calcite.interpreter.BindableRel;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.plan.ConventionTraitDef;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.plan.volcano.VolcanoPlanner;
import org.apache.calcite.prepare.CalciteCatalogReader;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.rules.CoreRules;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.schema.Table;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.sql.validate.SqlValidatorUtil;
import org.apache.calcite.sql2rel.SqlToRelConverter;
import org.apache.calcite.sql2rel.StandardConvertletTable;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Properties;
import java.util.function.BiConsumer;

/**
 * @author Frank Chen
 * @date 1/3/23 11:32 am
 */
@Service
public class QueryEngine {

    private final CalciteCatalogReader catalogReader;

    public QueryEngine() {
        CalciteSchema schema = CalciteSchema.createRootSchema(true);

        Properties props = new Properties();
        props.setProperty(CalciteConnectionProperty.CASE_SENSITIVE.camelName(), "false");
        CalciteConnectionConfig config = new CalciteConnectionConfigImpl(props);

        JavaTypeFactory typeFactory = new JavaTypeFactoryImpl();
        catalogReader = new CalciteCatalogReader(schema, Collections.singletonList(""), typeFactory, config);

    }

    public void addTable(String tableName, Table table) {
        this.catalogReader.getRootSchema().add(tableName, table);
    }

    public Enumerable<Object[]> executeSql(String sql, @Nullable BiConsumer<SqlNode, QueryContext> onParsed) throws Exception {
        QueryContext queryContext = new QueryContext(catalogReader.getRootSchema(),
                                                     (JavaTypeFactory) catalogReader.getTypeFactory());

        // Create a SQL parser to parse the query into AST
        SqlNode sqlNode = SqlParser.create(sql,
                                           SqlParser.config().withQuotedCasing(Casing.UNCHANGED).withUnquotedCasing(Casing.UNCHANGED))
                                   .parseQuery();

        if (onParsed != null) {
            onParsed.accept(sqlNode, queryContext);
        }

        SqlValidator validator = SqlValidatorUtil.newValidator(SqlStdOperatorTable.instance(),
                                                               catalogReader,
                                                               catalogReader.getTypeFactory(),
                                                               SqlValidator.Config.DEFAULT);

        //
        // Turn AST into logic plan
        //
        RelOptCluster cluster = newCluster(catalogReader.getTypeFactory());
        SqlToRelConverter relConverter = new SqlToRelConverter(NOOP_EXPANDER,
                                                               validator,
                                                               catalogReader,
                                                               cluster,
                                                               StandardConvertletTable.INSTANCE,
                                                               SqlToRelConverter.config());
        RelNode logicPlan = relConverter.convertQuery(sqlNode, true, true).rel;

        //
        // Initialize optimizer/planner with the necessary rules
        //
        RelOptPlanner planner = cluster.getPlanner();
        logicPlan = planner.changeTraits(logicPlan, cluster.traitSet().replace(BindableConvention.INSTANCE));
        planner.setRoot(logicPlan);

        //
        // Start the optimization process to obtain the most efficient physical plan based on the provided rule set.
        //
        BindableRel physicalPlan = (BindableRel) planner.findBestExp();

        // Run the executable plan using a context simply providing access to the schema
        return physicalPlan.bind(queryContext);
    }

    private static final RelOptTable.ViewExpander NOOP_EXPANDER = (rowType, queryString, schemaPath, viewPath) -> null;

    /**
     * Should be instance per execution
     */
    private static RelOptCluster newCluster(RelDataTypeFactory factory) {
        RelOptPlanner planner = new VolcanoPlanner();
        planner.addRelTraitDef(ConventionTraitDef.INSTANCE);
        planner.addRule(CoreRules.FILTER_INTO_JOIN);
        RelOptUtil.registerDefaultRules(planner, false, true);
        return RelOptCluster.create(planner, new RexBuilder(factory));
    }

}
