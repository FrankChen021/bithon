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

package org.bithon.server.alerting.manager.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.expt.InvalidExpressionException;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.model.IAlertInDepthExpressionVisitor;
import org.bithon.server.alerting.common.parser.AlertExpressionASTParser;
import org.bithon.server.alerting.manager.ManagerModuleEnabler;
import org.bithon.server.alerting.manager.api.parameter.ApiResponse;
import org.bithon.server.alerting.manager.api.parameter.ChangeLogVO;
import org.bithon.server.alerting.manager.api.parameter.GenericAlertByIdRequest;
import org.bithon.server.alerting.manager.api.parameter.GetAlertChangeLogListRequest;
import org.bithon.server.alerting.manager.api.parameter.GetAlertListRequest;
import org.bithon.server.alerting.manager.api.parameter.GetAlertListResponse;
import org.bithon.server.alerting.manager.api.parameter.GetAlertRecordByIdRequest;
import org.bithon.server.alerting.manager.api.parameter.GetAlertRecordListRequest;
import org.bithon.server.alerting.manager.api.parameter.GetAlertRecordListResponse;
import org.bithon.server.alerting.manager.api.parameter.GetChangeLogListResponse;
import org.bithon.server.alerting.manager.api.parameter.GetEvaluationLogsRequest;
import org.bithon.server.alerting.manager.api.parameter.GetEvaluationLogsResponse;
import org.bithon.server.alerting.manager.api.parameter.ListAlertVO;
import org.bithon.server.alerting.manager.biz.AlertExpressionSuggester;
import org.bithon.server.alerting.manager.biz.EvaluationLogService;
import org.bithon.server.alerting.manager.biz.JsonPayloadFormatter;
import org.bithon.server.commons.autocomplete.Suggestion;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.alerting.IAlertObjectStorage;
import org.bithon.server.storage.alerting.IAlertRecordStorage;
import org.bithon.server.storage.alerting.pojo.AlertChangeLogObject;
import org.bithon.server.storage.alerting.pojo.AlertRecordObject;
import org.bithon.server.storage.alerting.pojo.AlertStorageObject;
import org.bithon.server.storage.alerting.pojo.ListAlertDTO;
import org.bithon.server.storage.alerting.pojo.ListResult;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.query.Limit;
import org.bithon.server.storage.metrics.Interval;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/29
 */
@CrossOrigin
@RestController
@Conditional(ManagerModuleEnabler.class)
public class AlertQueryApi {

    final IAlertRecordStorage alertRecordStorage;
    final IAlertObjectStorage alertStorage;
    final EvaluationLogService evaluationLogService;
    final IDataSourceApi dataSourceApi;
    final ObjectMapper objectMapper;
    final AlertExpressionSuggester expressionSuggester;

    public AlertQueryApi(IAlertRecordStorage alertRecordStorage,
                         IAlertObjectStorage alertStorage,
                         EvaluationLogService evaluationLogService,
                         IDataSourceApi dataSourceApi,
                         ObjectMapper objectMapper) {
        this.alertRecordStorage = alertRecordStorage;
        this.alertStorage = alertStorage;
        this.evaluationLogService = evaluationLogService;
        this.dataSourceApi = dataSourceApi;
        this.objectMapper = objectMapper;
        this.expressionSuggester = new AlertExpressionSuggester(dataSourceApi);
    }

    @Data
    public static class ParseAlertExpressionRequest {
        @NotBlank
        private String expression;

        /**
         * Nullable
         */
        private String appName;
    }

    @Data
    @AllArgsConstructor
    public static class ParseAlertExpressionResponse {
        private Collection<AlertExpression> expressions;
    }

    @PostMapping("/api/alerting/alert/parse")
    public ApiResponse<ParseAlertExpressionResponse> parseAlertExpression(@Valid @RequestBody ParseAlertExpressionRequest request) {
        try {
            // Parse expression first
            IExpression alertExpression = AlertExpressionASTParser.parse(request.getExpression());

            // Get Schema for validation
            Map<String, ISchema> schemas = dataSourceApi.getSchemas();

            // Flatten expressions
            List<AlertExpression> alertExpressions = new ArrayList<>();
            alertExpression.accept((IAlertInDepthExpressionVisitor) expression -> {
                expression.getMetricExpression().validate(schemas);
                alertExpressions.add(expression);
            });

            return ApiResponse.success(new ParseAlertExpressionResponse(alertExpressions));
        } catch (InvalidExpressionException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @Data
    public static class SuggestAlertExpressionRequest {
        private String expression;
    }

    @Data
    public static class SuggestAlertExpressionResponse {
        private final Collection<Suggestion> suggestions;

        public SuggestAlertExpressionResponse(Collection<Suggestion> suggestions) {
            this.suggestions = suggestions;
        }
    }

    @PostMapping("/api/alerting/alert/suggest")
    public SuggestAlertExpressionResponse suggestAlertExpression(@Valid @RequestBody SuggestAlertExpressionRequest request) {
        Collection<Suggestion> suggestions = this.expressionSuggester.suggest(StringUtils.getOrEmpty(request.getExpression()));
        return new SuggestAlertExpressionResponse(suggestions);
    }

    public static class GetAlertRuleResponse extends AlertStorageObject {
        @Getter
        @Setter
        private Collection<AlertExpression> parsedExpressions;

        public GetAlertRuleResponse(AlertStorageObject alertStorageObject, Collection<AlertExpression> parsedExpressions) {
            setId(alertStorageObject.getId());
            setName(alertStorageObject.getName());
            setAppName(alertStorageObject.getAppName());
            setNamespace(alertStorageObject.getNamespace());
            setDisabled(alertStorageObject.isDisabled());
            setDeleted(alertStorageObject.isDeleted());
            setPayload(alertStorageObject.getPayload());
            setCreatedAt(alertStorageObject.getCreatedAt());
            setUpdatedAt(alertStorageObject.getUpdatedAt());
            setLastOperator(alertStorageObject.getLastOperator());
            this.parsedExpressions = parsedExpressions;
        }
    }

    @PostMapping("/api/alerting/alert/get")
    public ApiResponse<GetAlertRuleResponse> getAlertById(@Valid @RequestBody GenericAlertByIdRequest request) {
        AlertStorageObject ruleObject = alertStorage.getAlertById(request.getAlertId());

        // Parse expression first
        if (ruleObject != null) {
            IExpression alertExpression = AlertExpressionASTParser.parse(ruleObject.getPayload().getExpr());

            // Get Schema for validation
            Map<String, ISchema> schemas = dataSourceApi.getSchemas();

            // Flatten expressions
            List<AlertExpression> alertExpressions = new ArrayList<>();
            alertExpression.accept((IAlertInDepthExpressionVisitor) expression -> {
                expression.getMetricExpression().validate(schemas);
                alertExpressions.add(expression);
            });
            return ApiResponse.success(new GetAlertRuleResponse(ruleObject, alertExpressions));
        }
        return ApiResponse.fail("Alert rule not found");
    }

    @PostMapping("/api/alerting/alert/list")
    public GetAlertListResponse getAlerts(@Valid @RequestBody GetAlertListRequest request) {
        request.getOrderBy().setName(StringUtils.camelToSnake(request.getOrderBy().getName()));

        List<ListAlertDTO> alertList = alertStorage.getAlertList(request.getAppName(),
                                                                 request.getAlertName(),
                                                                 request.getOrderBy(),
                                                                 request.getLimit());

        return new GetAlertListResponse(alertStorage.getAlertListSize(request.getAppName(), request.getAlertName()),
                                        alertList.stream()
                                                 .map(alert -> {
                                                     ListAlertVO vo = new ListAlertVO();
                                                     vo.setAlertId(alert.getAlertId());
                                                     vo.setName(alert.getAlertName());
                                                     vo.setAppName(alert.getAppName());
                                                     vo.setEnabled(!alert.isDisabled());
                                                     vo.setCreatedAt(alert.getCreatedAt().getTime());
                                                     vo.setUpdatedAt(alert.getUpdatedAt().getTime());
                                                     vo.setLastAlertAt(alert.getLastAlertAt() == null ? 0L : alert.getLastAlertAt().getTime());
                                                     vo.setLastOperator(alert.getLastOperator());
                                                     vo.setLastRecordId(alert.getLastRecordId());
                                                     vo.setAlertStatus(alert.getAlertStatus());
                                                     return vo;
                                                 })
                                                 .collect(Collectors.toList()));
    }

    @PostMapping("/api/alerting/alert/record/get")
    public ApiResponse<AlertRecordObject> getAlertRecordById(@Valid @RequestBody GetAlertRecordByIdRequest request) {
        AlertRecordObject recordObject = alertRecordStorage.getAlertRecord(request.getId());
        return recordObject == null ? ApiResponse.fail(StringUtils.format("Record [%s] does not exist.", request.getId())) : ApiResponse.success(recordObject);
    }

    @PostMapping("/api/alerting/alert/record/list")
    public GetAlertRecordListResponse getRecordList(@Valid @RequestBody GetAlertRecordListRequest request) {
        Interval interval = null;
        if (request.getInterval() != null) {
            interval = Interval.of(request.getInterval().getStartISO8601(), request.getInterval().getEndISO8601());
        }
        Limit limit = null;
        if (request.getPageNumber() != null && request.getPageSize() != null) {
            limit = new Limit((int) request.getPageSize(), request.getPageNumber() * request.getPageSize());
        }

        ListResult<AlertRecordObject> results = alertRecordStorage.getAlertRecords(request.getAlertId(), interval, limit);
        return new GetAlertRecordListResponse(results.getRows(),
                                              results.getData());
    }

    @PostMapping("/api/alerting/alert/change-log/get")
    public GetChangeLogListResponse getChangeLogs(@Valid @RequestBody GetAlertChangeLogListRequest request) {
        ListResult<AlertChangeLogObject> results = alertStorage.getChangeLogs(request.getAlertId(),
                                                                              request.getPageNumber(),
                                                                              request.getPageSize());

        // Add a newline to the expr so that the YAML will render it in block style
        Function<Object, Object> expressionTransformer = (obj) -> {
            if (obj instanceof Map) {
                String expr = (String) ((Map) obj).get("expr");
                if (expr != null) {
                    ((Map) obj).put("expr", expr + "\n");
                }
            }
            return obj;
        };

        JsonPayloadFormatter formatter = JsonPayloadFormatter.get(request.getFormat());
        return new GetChangeLogListResponse(results.getRows(),
                                            results.getData()
                                                   .stream()
                                                   .map(log -> {
                                                       ChangeLogVO vo = new ChangeLogVO();
                                                       BeanUtils.copyProperties(log, vo);
                                                       vo.setPayloadBefore(formatter.format(log.getPayloadBefore(), this.objectMapper, expressionTransformer));
                                                       vo.setPayloadAfter(formatter.format(log.getPayloadAfter(), this.objectMapper, expressionTransformer));
                                                       vo.setTimestamp(log.getCreatedAt().getTime());
                                                       return vo;
                                                   }).collect(Collectors.toList()));
    }

    @PostMapping("/api/alerting/alert/evaluation-log/get")
    public GetEvaluationLogsResponse getEvaluationLogs(@Valid @RequestBody GetEvaluationLogsRequest request) {
        return this.evaluationLogService.getEvaluationLogs(request.getAlertId(),
                                                           TimeSpan.fromISO8601(request.getInterval().getStartISO8601()),
                                                           TimeSpan.fromISO8601(request.getInterval().getEndISO8601()));
    }
}
