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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.Table;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlCharStringLiteral;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.SqlUpdate;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.util.SqlBasicVisitor;
import org.apache.calcite.sql.validate.SqlValidatorException;
import org.apache.calcite.util.NlsString;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.discovery.client.DiscoveredServiceInvoker;
import org.bithon.server.web.service.WebServiceModuleEnabler;
import org.bithon.server.web.service.agent.sql.AgentSchema;
import org.bithon.server.web.service.agent.sql.table.IPushdownPredicateProvider;
import org.bithon.server.web.service.common.output.IOutputFormatter;
import org.bithon.server.web.service.common.output.JsonCompactOutputFormatter;
import org.bithon.server.web.service.common.output.TabSeparatedOutputFormatter;
import org.bithon.server.web.service.common.sql.SqlExecutionContext;
import org.bithon.server.web.service.common.sql.SqlExecutionEngine;
import org.bithon.server.web.service.common.sql.SqlExecutionResult;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Frank Chen
 * @date 23/2/23 9:10 pm
 */
@CrossOrigin
@RestController
@Conditional(WebServiceModuleEnabler.class)
public class AgentDiagnosisApi {

    private final SqlExecutionEngine sqlExecutionEngine;
    private final ObjectMapper objectMapper;

    public AgentDiagnosisApi(DiscoveredServiceInvoker discoveredServiceInvoker,
                             SqlExecutionEngine sqlExecutionEngine,
                             ObjectMapper objectMapper,
                             ApplicationContext applicationContext) {
        this.objectMapper = objectMapper;
        this.sqlExecutionEngine = sqlExecutionEngine;
        this.sqlExecutionEngine.addSchema("agent", new AgentSchema(discoveredServiceInvoker, applicationContext));
    }

    @PostMapping(value = "/api/agent/query")
    public void query(@Valid @RequestBody String query,
                      HttpServletRequest httpServletRequest,
                      HttpServletResponse httpResponse) throws Exception {
        if ("application/x-www-form-urlencoded".equals(httpServletRequest.getHeader("Content-Type"))) {
            throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(),
                                            "Content-Type with application/x-www-form-urlencoded is not accepted. Please use text/plain instead.");
        }

        SqlExecutionResult result = this.sqlExecutionEngine.executeSql(query, (sqlNode, queryContext) -> {
            SqlNode whereNode;
            SqlNode from = null;
            if (sqlNode.getKind() == SqlKind.ORDER_BY) {
                whereNode = ((SqlSelect) ((SqlOrderBy) sqlNode).query).getWhere();
                from = ((SqlSelect) ((SqlOrderBy) sqlNode).query).getFrom();
            } else if (sqlNode.getKind() == SqlKind.SELECT) {
                whereNode = ((SqlSelect) (sqlNode)).getWhere();
                from = ((SqlSelect) (sqlNode)).getFrom();
            } else if (sqlNode.getKind() == SqlKind.UPDATE) {
                whereNode = ((SqlUpdate) (sqlNode)).getCondition();
            } else {
                throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(), "Unsupported SQL Kind: %s", sqlNode.getKind());
            }

            Map<String, Boolean> pushdownPredicates = Collections.emptyMap();
            if (from != null) {
                if (!(from instanceof SqlIdentifier)) {
                    throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(), "Not supported '%s'. The 'from' clause can only be an identifier", from.toString());
                }

                List<String> names = ((SqlIdentifier) from).names;
                if (names.size() != 2) {
                    throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(), "Unknown identifier: %s", from.toString());
                }

                Schema schema = queryContext.getRootSchema().getSubSchema(names.get(0));
                if (schema == null) {
                    throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(), "Unknown schema: %s", names.get(0));
                }

                Table table = schema.getTable(names.get(1));
                if (table == null) {
                    throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(), "Unknown table: %s", names.get(1));
                }
                if (table instanceof IPushdownPredicateProvider) {
                    pushdownPredicates = ((IPushdownPredicateProvider) table).getPredicates();
                }
            }
            if (whereNode != null) {
                // Convert related filter at the raw SQL into query context parameters
                whereNode.accept(new FilterToContextParameterConverter(queryContext, pushdownPredicates));
            }

            if (!pushdownPredicates.isEmpty()) {
                pushdownPredicates.forEach((predicate, required) -> {
                    if (required && queryContext.get(predicate) == null) {
                        throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(),
                                                        "The filter '%s' is required but not provided",
                                                        predicate);
                    }
                });
            }
        });

        IOutputFormatter formatter;
        String acceptEncoding = httpServletRequest.getHeader("Accept");
        if (acceptEncoding != null && acceptEncoding.contains("application/json")) {
            formatter = new JsonCompactOutputFormatter(this.objectMapper);
        } else {
            formatter = new TabSeparatedOutputFormatter();
        }
        httpResponse.addHeader("Content-Type", formatter.getContentType());
        formatter.format(httpResponse.getWriter(), result.fields, result.rows);
    }

    private static class FilterToContextParameterConverter extends SqlBasicVisitor<String> {
        private final SqlExecutionContext queryContext;
        private final Map<String, Boolean> pushDownPredicates;

        public FilterToContextParameterConverter(SqlExecutionContext queryContext, Map<String, Boolean> pushDownPredicates) {
            this.queryContext = queryContext;
            this.pushDownPredicates = pushDownPredicates;
        }

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
            SqlNode identifierNode = call.getOperandList().get(0);
            SqlNode literal = call.getOperandList().get(1);
            if (!(identifierNode instanceof SqlIdentifier)) {
                SqlNode tmp = literal;
                literal = identifierNode;
                identifierNode = tmp;
            }
            if (!(identifierNode instanceof SqlIdentifier)) {
                return super.visit(call);
            }

            String identifier = ((SqlIdentifier) identifierNode).getSimple().toLowerCase(Locale.ENGLISH);
            if (!pushDownPredicates.containsKey(identifier)) {
                return super.visit(call);
            }

            if (!(literal instanceof SqlCharStringLiteral)) {
                throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(),
                                                StringUtils.format("Operand for [%s] must be type of STRING", identifier));
            }

            // Set the instance/_token in the execution context
            this.queryContext.set(identifier, ((SqlCharStringLiteral) literal).getValueAs(NlsString.class).getValue());

            // Replace current filter expression by '1 = 1'
            call.setOperand(0, SqlLiteral.createBoolean(true, new SqlParserPos(-1, -1)));
            call.setOperand(1, SqlLiteral.createBoolean(true, new SqlParserPos(-1, -1)));
            return null;
        }
    }

    @ExceptionHandler({SqlValidatorException.class, SqlParseException.class})
    void suppressSqlException(HttpServletResponse response, Exception e) throws IOException {
        response.setContentType("text/plain");
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.getWriter().write(e.getMessage());
    }
}
