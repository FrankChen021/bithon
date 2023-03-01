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

package org.bithon.server.web.service.agent.api;

import lombok.Getter;
import org.apache.calcite.DataContext;
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
import org.apache.calcite.linq4j.Linq4j;
import org.apache.calcite.linq4j.QueryProvider;
import org.apache.calcite.plan.ConventionTraitDef;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.plan.volcano.VolcanoPlanner;
import org.apache.calcite.prepare.CalciteCatalogReader;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.rules.CoreRules;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.schema.ScannableTable;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlCharStringLiteral;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.util.SqlBasicVisitor;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.sql.validate.SqlValidatorUtil;
import org.apache.calcite.sql2rel.SqlToRelConverter;
import org.apache.calcite.sql2rel.StandardConvertletTable;
import org.apache.calcite.util.NlsString;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.discovery.client.ServiceBroadcastInvoker;
import org.bithon.server.discovery.declaration.ServiceResponse;
import org.bithon.server.discovery.declaration.cmd.CommandArgs;
import org.bithon.server.discovery.declaration.cmd.IAgentCommandApi;
import org.bithon.server.web.service.WebServiceModuleEnabler;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 23/2/23 9:10 pm
 */
@CrossOrigin
@RestController
@Conditional(WebServiceModuleEnabler.class)
public class AgentCommandDelegationApi {

    private final IAgentCommandApi impl;

    public AgentCommandDelegationApi(ServiceBroadcastInvoker serviceBroadcastInvoker) {
        this.impl = serviceBroadcastInvoker.create(IAgentCommandApi.class);
    }

    private abstract static class BaseTable extends AbstractTable implements ScannableTable {
        private RelDataType rowType;

        protected abstract java.lang.Class getRecordClazz();

        @Override
        public RelDataType getRowType(final RelDataTypeFactory typeFactory) {
            if (rowType == null) {
                rowType = typeFactory.createJavaType(getRecordClazz());
            }
            return rowType;
        }

        @Override
        public Enumerable<Object[]> scan(final DataContext root) {
            return Linq4j.asEnumerable(getData((QueryContext) root).stream()
                                                                   .map((IAgentCommandApi.IObjectArrayConvertable::toObjectArray))
                                                                   .collect(Collectors.toList()));
        }

        protected abstract <T extends IAgentCommandApi.IObjectArrayConvertable> List<T> getData(QueryContext queryContext);
    }

    private static class InstanceTable extends BaseTable {
        private final IAgentCommandApi impl;

        InstanceTable(IAgentCommandApi impl) {
            this.impl = impl;
        }

        @Override
        protected List<IAgentCommandApi.IObjectArrayConvertable> getData(QueryContext queryContext) {
            ServiceResponse<IAgentCommandApi.InstanceRecord> clients = impl.getClients();
            if (clients.getError() != null) {
                throw new RuntimeException(clients.getError().toString());
            }
            return (List<IAgentCommandApi.IObjectArrayConvertable>)(List<?>)clients.getRows();
        }

        @Override
        protected java.lang.Class getRecordClazz() {
            return IAgentCommandApi.InstanceRecord.class;
        }
    }

    private static class ClassTable extends BaseTable {
        private final IAgentCommandApi impl;

        ClassTable(IAgentCommandApi impl) {
            this.impl = impl;
        }

        @Override
        protected List<IAgentCommandApi.IObjectArrayConvertable> getData(QueryContext queryContext) {

            ServiceResponse<IAgentCommandApi.ClassRecord> classList = impl.getClass((CommandArgs<Void>) queryContext.commandArgs);
            if (classList.getError() != null) {
                throw new RuntimeException(classList.getError().toString());
            }

            return (List<IAgentCommandApi.IObjectArrayConvertable>)(List<?>)classList.getRows();
        }

        @Override
        protected Class getRecordClazz() {
            return IAgentCommandApi.ClassRecord.class;
        }
    }

    private static class StackTraceTable extends BaseTable {
        private final IAgentCommandApi impl;

        StackTraceTable(IAgentCommandApi impl) {
            this.impl = impl;
        }

        @Override
        protected List<IAgentCommandApi.IObjectArrayConvertable> getData(QueryContext queryContext) {
            ServiceResponse<IAgentCommandApi.StackTraceRecord> stackTraceList = impl.getStackTrace((CommandArgs<Void>) queryContext.commandArgs);
            if (stackTraceList.getError() != null) {
                throw new RuntimeException(stackTraceList.getError().toString());
            }

            return (List<IAgentCommandApi.IObjectArrayConvertable>)(List<?>)stackTraceList.getRows();
        }

        @Override
        protected Class getRecordClazz() {
            return IAgentCommandApi.StackTraceRecord.class;
        }
    }

    private static class ConfigurationTable extends BaseTable {
        private final IAgentCommandApi impl;

        ConfigurationTable(IAgentCommandApi impl) {
            this.impl = impl;
        }

        @Override
        protected List<IAgentCommandApi.IObjectArrayConvertable> getData(QueryContext queryContext) {
            ServiceResponse<IAgentCommandApi.ConfigurationRecord> configurations = impl.getConfiguration((CommandArgs<IAgentCommandApi.GetConfigurationRequest>) queryContext.commandArgs);
            if (configurations.getError() != null) {
                throw new RuntimeException(configurations.getError().toString());
            }

            return (List<IAgentCommandApi.IObjectArrayConvertable>)(List<?>) configurations.getRows();
        }

        @Override
        protected Class getRecordClazz() {
            return IAgentCommandApi.ConfigurationRecord.class;
        }
    }

    @GetMapping(value = "/api/agent/query", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void query(@Valid @RequestBody String query, HttpServletResponse httpResponse) throws Exception {
        // Create a SQL parser to parse the query into AST
        SqlNode sqlNode = SqlParser.create(query,
                                           SqlParser.config().withQuotedCasing(Casing.UNCHANGED).withUnquotedCasing(Casing.UNCHANGED))
                                   .parseQuery();

        //
        // appId is an always pushed down filter
        // We remove it from SQL because this specific field does not exist on all tables
        //
        GetAndRemoveAppIdFilter appIdFilter = new GetAndRemoveAppIdFilter();
        SqlNode whereNode;
        if (sqlNode.getKind() == SqlKind.ORDER_BY) {
            whereNode = ((SqlSelect) ((SqlOrderBy) sqlNode).query).getWhere();
        } else if (sqlNode.getKind() == SqlKind.SELECT) {
            whereNode = ((SqlSelect) (sqlNode)).getWhere();
        } else {
            throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(), "Unsupported SQL Kind: %s", sqlNode.getKind());
        }
        if (whereNode != null) {
            whereNode.accept(appIdFilter);
        }

        CalciteSchema schema = CalciteSchema.createRootSchema(true);
        schema.add("instance", new InstanceTable(impl));
        schema.add("class", new ClassTable(impl));
        schema.add("stack_trace", new StackTraceTable(impl));
        schema.add("configuration", new ConfigurationTable(impl));

        Properties props = new Properties();
        props.setProperty(CalciteConnectionProperty.CASE_SENSITIVE.camelName(), "false");
        CalciteConnectionConfig config = new CalciteConnectionConfigImpl(props);

        JavaTypeFactory typeFactory = new JavaTypeFactoryImpl();
        CalciteCatalogReader catalogReader = new CalciteCatalogReader(schema, Collections.singletonList(""), typeFactory, config);

        SqlValidator validator = SqlValidatorUtil.newValidator(SqlStdOperatorTable.instance(), catalogReader, typeFactory, SqlValidator.Config.DEFAULT);

        //
        // Turn AST into logic plan
        //
        RelOptCluster cluster = newCluster(typeFactory);
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

        PrintWriter pw = httpResponse.getWriter();

        // Run the executable plan using a context simply providing access to the schema
        for (Object[] row : physicalPlan.bind(new QueryContext(schema,
                                                               typeFactory,
                                                               new CommandArgs<Void>(appIdFilter.getAppId(), null)))) {
            for (int i = 0; i < row.length; i++) {
                Object cell = row[i];
                if (cell == null) {
                    pw.write("NULL");
                } else {
                    String c = cell.toString();
                    if (c.indexOf('\n') > 0) {
                        String indent = "\n";
                        for (int j = 0; j < i; j++) {
                            indent += '\t';
                        }
                        c = c.replaceAll("\n", indent);
                    }
                    pw.write(c);
                }
                pw.write('\t');
            }
            pw.write('\n');
        }
    }

    private static final class QueryContext implements DataContext {
        private final SchemaPlus schema;
        private final JavaTypeFactory typeFactory;
        private final CommandArgs<?> commandArgs;

        QueryContext(CalciteSchema calciteSchema, JavaTypeFactory typeFactory, CommandArgs<?> commandArgs) {
            this.schema = calciteSchema.plus();
            this.typeFactory = typeFactory;
            this.commandArgs = commandArgs;
        }

        @Override
        public SchemaPlus getRootSchema() {
            return schema;
        }

        @Override
        public JavaTypeFactory getTypeFactory() {
            return typeFactory;
        }

        @Override
        public QueryProvider getQueryProvider() {
            return null;
        }

        @Override
        public Object get(final String name) {
            return null;
        }
    }

    private static final RelOptTable.ViewExpander NOOP_EXPANDER = (rowType, queryString, schemaPath, viewPath) -> null;

    private static RelOptCluster newCluster(RelDataTypeFactory factory) {
        RelOptPlanner planner = new VolcanoPlanner();
        planner.addRelTraitDef(ConventionTraitDef.INSTANCE);
        planner.addRule(CoreRules.FILTER_INTO_JOIN);
        RelOptUtil.registerDefaultRules(planner, false, true);
        return RelOptCluster.create(planner, new RexBuilder(factory));
    }

    private void printError(HttpServletResponse httpResponse, ServiceResponse.Error error) throws IOException {
        PrintWriter pw = httpResponse.getWriter();

        pw.write(StringUtils.format("uri: %s\n", error.getUri()));
        pw.write(StringUtils.format("exception: %s\n", error.getException()));
        pw.write(StringUtils.format("message: %s\n", error.getMessage()));
    }

    private static class GetAndRemoveAppIdFilter extends SqlBasicVisitor<String> {
        @Getter
        private String appId;

        @Override
        public String visit(SqlCall call) {
            if (!(call instanceof SqlBasicCall)) {
                return super.visit(call);
            }

            if (!"=".equals(call.getOperator().getName())) {
                return super.visit(call);
            }

            if (call.getOperandList().size() != 2) {
                return super.visit(call);
            }
            SqlNode identifier = call.getOperandList().get(0);
            SqlNode literal = call.getOperandList().get(1);
            if (!(identifier instanceof SqlIdentifier)) {
                SqlNode tmp = literal;
                literal = identifier;
                identifier = tmp;
            }
            if (!(identifier instanceof SqlIdentifier)) {
                return super.visit(call);
            }
            if (!"appId".equalsIgnoreCase(((SqlIdentifier) identifier).getSimple())) {
                return super.visit(call);
            }

            if (!(literal instanceof SqlCharStringLiteral)) {
                throw new RuntimeException("xxx");
            }

            appId = ((SqlCharStringLiteral) literal).getValueAs(NlsString.class).getValue();
            call.setOperand(0, SqlLiteral.createBoolean(true, new SqlParserPos(-1, -1)));
            call.setOperand(1, SqlLiteral.createBoolean(true, new SqlParserPos(-1, -1)));
            return null;
        }
    }
}
