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

import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlCharStringLiteral;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.util.SqlBasicVisitor;
import org.apache.calcite.util.NlsString;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.server.discovery.client.ServiceBroadcastInvoker;
import org.bithon.server.discovery.declaration.cmd.IAgentCommandApi;
import org.bithon.server.web.service.WebServiceModuleEnabler;
import org.bithon.server.web.service.agent.service.AgentSchema;
import org.bithon.server.web.service.common.sql.QueryContext;
import org.bithon.server.web.service.common.sql.QueryEngine;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.PrintWriter;

/**
 * @author Frank Chen
 * @date 23/2/23 9:10 pm
 */
@CrossOrigin
@RestController
@Conditional(WebServiceModuleEnabler.class)
public class AgentCommandDelegationApi {

    private final QueryEngine queryEngine;

    public AgentCommandDelegationApi(ServiceBroadcastInvoker serviceBroadcastInvoker,
                                     QueryEngine queryEngine) {
        IAgentCommandApi impl = serviceBroadcastInvoker.create(IAgentCommandApi.class);
        this.queryEngine = queryEngine;
        this.queryEngine.addSchema("agent", new AgentSchema(impl));
    }

    @GetMapping(value = "/api/agent/query", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void query(@Valid @RequestBody String query, HttpServletResponse httpResponse) throws Exception {
        Enumerable<Object[]> rows = this.queryEngine.executeSql(query, (sqlNode, queryContext) -> {
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
            } else {
                throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(), "Unsupported SQL Kind: %s", sqlNode.getKind());
            }
            if (whereNode != null) {
                whereNode.accept(appIdFilter);
            }
        });

        PrintWriter pw = httpResponse.getWriter();
        for (Object[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                Object cell = row[i];
                if (cell == null) {
                    pw.write("NULL");
                } else {
                    String c = cell.toString();
                    if (c.indexOf('\n') > 0) {
                        StringBuilder indent = new StringBuilder("\n");
                        for (int j = 0; j < i; j++) {
                            indent.append('\t');
                        }
                        c = c.replaceAll("\n", indent.toString());
                    }
                    pw.write(c);
                }
                pw.write('\t');
            }
            pw.write('\n');
        }
    }

    private static class GetAndRemoveAppIdFilter extends SqlBasicVisitor<String> {
        private final QueryContext queryContext;

        public GetAndRemoveAppIdFilter(QueryContext queryContext) {
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

            this.queryContext.set("appId", ((SqlCharStringLiteral) literal).getValueAs(NlsString.class).getValue());
            call.setOperand(0, SqlLiteral.createBoolean(true, new SqlParserPos(-1, -1)));
            call.setOperand(1, SqlLiteral.createBoolean(true, new SqlParserPos(-1, -1)));
            return null;
        }
    }
}
