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

import com.google.common.collect.ImmutableMap;
import org.bithon.agent.rpc.brpc.cmd.ILoggingCommand;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.expression.BinaryExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionVisitor;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.logging.LoggerConfiguration;
import org.bithon.component.commons.logging.LoggingLevel;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.discovery.declaration.controller.IAgentControllerApi;
import org.bithon.server.web.service.common.sql.SqlExecutionContext;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/4/2 16:20
 */
public class LoggerTable extends AbstractBaseTable implements IUpdatableTable, IPushdownPredicateProvider {
    private final AgentServiceProxyFactory proxyFactory;

    public LoggerTable(AgentServiceProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    @Override
    public Map<String, Boolean> getPredicates() {
        return ImmutableMap.of(IAgentControllerApi.PARAMETER_NAME_INSTANCE, true);
    }

    @Override
    protected List<Object[]> getData(SqlExecutionContext executionContext) {
        return proxyFactory.create(executionContext.getParameters(),
                                   ILoggingCommand.class)
                           .getLoggers()
                           .stream()
                           .map(LoggerConfiguration::toObjects)
                           .collect(Collectors.toList());
    }

    @Override
    protected Class<?> getRecordClazz() {
        return LoggerConfigurationRecord.class;
    }

    public static class LoggerConfigurationRecord {

        public String name;

        // For the Schema, the level is declared as String to avoid the Enum problem at Calcite side
        public String level;
        public String effectiveLevel;
    }

    static class OneFilter implements IExpressionVisitor {
        private String name;
        private Object value;

        @Override
        public boolean visit(LiteralExpression expression) {
            value = expression.getLiteralValue();
            return false;
        }

        @Override
        public boolean visit(IdentifierExpression expression) {
            name = expression.getIdentifier();
            return false;
        }
    }

    @Override
    public int update(SqlExecutionContext executionContext,
                      IExpression filterExpression,
                      Map<String, Object> newValues) {
        String token = (String) executionContext.get("_token");
        Preconditions.checkNotNull(token, "'_token' is missed in the WHERE clause.");

        Preconditions.checkNotNull(filterExpression, "'name' is missed in the WHERE clause.");
        Preconditions.checkIfTrue(filterExpression instanceof BinaryExpression, "WHERE clause must only contain one filter.");

        BinaryExpression binaryExpression = (BinaryExpression) filterExpression;
        Preconditions.checkIfTrue("=".equals(binaryExpression.getType()), "Logger table does not support operator '%s', only '=' is supported", binaryExpression.getType());

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

        return proxyFactory.create(executionContext.getParameters(),
                                   ILoggingCommand.class)
                           .setLogger((String) nameFilter.value, loggingLevel)
                           .stream().reduce(0, Integer::sum);
    }
}
