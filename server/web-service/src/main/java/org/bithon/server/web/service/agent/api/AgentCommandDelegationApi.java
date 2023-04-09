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
import org.bithon.server.discovery.client.ServiceBroadcastInvoker;
import org.bithon.server.discovery.declaration.cmd.IAgentCommandApi;
import org.bithon.server.web.service.WebServiceModuleEnabler;
import org.bithon.server.web.service.agent.sql.AgentSchema;
import org.bithon.server.web.service.common.output.IOutputFormatter;
import org.bithon.server.web.service.common.output.JsonCompactOutputFormatter;
import org.bithon.server.web.service.common.output.TabSeparatedOutputFormatter;
import org.bithon.server.web.service.common.sql.SqlExecutionContext;
import org.bithon.server.web.service.common.sql.SqlExecutionEngine;
import org.bithon.server.web.service.common.sql.SqlExecutionResult;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;

/**
 * @author Frank Chen
 * @date 23/2/23 9:10 pm
 */
@CrossOrigin
@RestController
@Conditional(WebServiceModuleEnabler.class)
public class AgentCommandDelegationApi {

    private final SqlExecutionEngine sqlExecutionEngine;
    private final ObjectMapper objectMapper;

    public AgentCommandDelegationApi(ServiceBroadcastInvoker serviceBroadcastInvoker,
                                     SqlExecutionEngine sqlExecutionEngine,
                                     ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.sqlExecutionEngine = sqlExecutionEngine;
        this.sqlExecutionEngine.addSchema("agent", new AgentSchema(serviceBroadcastInvoker));
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
            //
            // appId is an always pushed down filter
            // We remove it from SQL because this specific field does not exist on all tables
            //
            GetAndRemoveAppIdFilter appIdFilter = new GetAndRemoveAppIdFilter(queryContext);
            SqlNode whereNode;
            if (sqlNode.getKind() == SqlKind.ORDER_BY) {
                whereNode = ((SqlSelect) ((SqlOrderBy) sqlNode).query).getWhere();
            } else if (sqlNode.getKind() == SqlKind.SELECT) {
                whereNode = ((SqlSelect) (sqlNode)).getWhere();
            } else if (sqlNode.getKind() == SqlKind.UPDATE) {
                whereNode = ((SqlUpdate) (sqlNode)).getCondition();
            } else {
                throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(), "Unsupported SQL Kind: %s", sqlNode.getKind());
            }
            if (whereNode != null) {
                whereNode.accept(appIdFilter);
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

    private static class GetAndRemoveAppIdFilter extends SqlBasicVisitor<String> {
        private final SqlExecutionContext queryContext;

        public GetAndRemoveAppIdFilter(SqlExecutionContext queryContext) {
            this.queryContext = queryContext;
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

            String identifier = ((SqlIdentifier) identifierNode).getSimple();
            if (!"appId".equalsIgnoreCase(identifier) && !"_token".equalsIgnoreCase(identifier)) {
                return super.visit(call);
            }

            if (!(literal instanceof SqlCharStringLiteral)) {
                throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(),
                                                StringUtils.format("Operand for [%s] must be type of STRING", identifier));
            }

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
