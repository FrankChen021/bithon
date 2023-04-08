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

package org.bithon.server.web.service.agent.sql.table;

import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.expression.BinaryExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionVisitor;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.logging.LoggingLevel;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.discovery.declaration.ServiceResponse;
import org.bithon.server.discovery.declaration.cmd.CommandArgs;
import org.bithon.server.discovery.declaration.cmd.IAgentCommandApi;
import org.bithon.server.web.service.common.sql.SqlExecutionContext;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/4/2 16:20
 */
public class LoggerTable extends AbstractBaseTable implements IUpdatableTable {
    private final IAgentCommandApi impl;

    public LoggerTable(IAgentCommandApi impl) {
        this.impl = impl;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<IAgentCommandApi.IObjectArrayConvertable> getData(SqlExecutionContext executionContext) {
        String appId = (String) executionContext.get("appId");
        Preconditions.checkNotNull(appId, "'appId' is missed in the query filter");

        ServiceResponse<IAgentCommandApi.LoggerConfigurationRecord> records = impl.getLoggerList(new CommandArgs<>(appId));
        if (records.getError() != null) {
            throw new RuntimeException(records.getError().toString());
        }

        return (List<IAgentCommandApi.IObjectArrayConvertable>) (List<?>) records.getRows();
    }

    @Override
    protected Class<?> getRecordClazz() {
        return IAgentCommandApi.LoggerConfigurationRecord.class;
    }

    static class OneFilter implements IExpressionVisitor<Void> {
        private String name;
        private Object value;

        @Override
        public Void visit(LiteralExpression expression) {
            value = expression.getValue();
            return null;
        }

        @Override
        public Void visit(IdentifierExpression expression) {
            name = expression.getIdentifier();
            return null;
        }
    }

    @Override
    public int update(SqlExecutionContext executionContext,
                      IExpression filterExpression,
                      Map<String, Object> newValues) {
        String appId = (String) executionContext.get("appId");
        Preconditions.checkNotNull(appId, "'appId' is missed in the WHERE clause.");

        String token = (String) executionContext.get("_token");
        Preconditions.checkNotNull(token, "'_token' is missed in the WHERE clause.");

        Preconditions.checkNotNull(filterExpression, "'name' is missed in the WHERE clause.");
        Preconditions.checkIfTrue(filterExpression instanceof BinaryExpression, "WHERE clause must only contain one filter.");

        BinaryExpression binaryExpression = (BinaryExpression) filterExpression;
        Preconditions.checkIfTrue("=".equals(binaryExpression.getOperator()), "Logger table does not support operator '%s', only '=' is supported", binaryExpression.getOperator());

        OneFilter nameFilter = new OneFilter();
        binaryExpression.getLeft().accept(nameFilter);
        binaryExpression.getRight().accept(nameFilter);
        Preconditions.checkNotNull(nameFilter.name, "WHERE clause must contains a filter");
        Preconditions.checkIfTrue("name".equals(nameFilter.name), "WHERE clause must only contain a filter that works on 'name' field");
        Preconditions.checkIfTrue(nameFilter.value instanceof String, "Filter on 'name' field must compare to type of STRING");

        String newLevel = (String) newValues.get("level");
        if (newLevel == null) {
            throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(),
                                            "'level' is not updated");
        }

        LoggingLevel loggingLevel;
        try {
            loggingLevel = LoggingLevel.valueOf(newLevel.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException ignored) {
            throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(),
                                            "Given level [%s] is not a valid value, must be one of []",
                                            newLevel);
        }

        if (newValues.size() > 1) {
            throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(),
                                            "Only 'level' is allowed to updated");
        }

        IAgentCommandApi.SetLoggerArgs args = new IAgentCommandApi.SetLoggerArgs();
        args.setLevel(loggingLevel);
        args.setName((String) nameFilter.value);
        ServiceResponse<IAgentCommandApi.ModifiedRecord> result = impl.setLogger(new CommandArgs<>(appId, token, args));
        if (result.getError() != null) {
            throw new HttpMappableException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                            result.getError().toString());
        }

        int totalRows = 0;
        for (IAgentCommandApi.ModifiedRecord record : result.getRows()) {
            totalRows += record.getRows();
        }
        return totalRows;
    }
}
