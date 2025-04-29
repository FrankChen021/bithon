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
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.common.model.IAlertInDepthExpressionVisitor;
import org.bithon.server.alerting.common.parser.AlertExpressionASTParser;
import org.bithon.server.alerting.common.utils.Validator;
import org.bithon.server.alerting.manager.ManagerModuleEnabler;
import org.bithon.server.alerting.manager.api.parameter.ApiResponse;
import org.bithon.server.alerting.manager.api.parameter.ChangeLogVO;
import org.bithon.server.alerting.manager.api.parameter.CreateAlertRuleRequest;
import org.bithon.server.alerting.manager.api.parameter.GenericAlertByIdRequest;
import org.bithon.server.alerting.manager.api.parameter.GetAlertChangeLogListRequest;
import org.bithon.server.alerting.manager.api.parameter.GetAlertListRequest;
import org.bithon.server.alerting.manager.api.parameter.GetAlertListResponse;
import org.bithon.server.alerting.manager.api.parameter.GetChangeLogListResponse;
import org.bithon.server.alerting.manager.api.parameter.GetRuleFoldersRequest;
import org.bithon.server.alerting.manager.api.parameter.GetRuleFoldersResponse;
import org.bithon.server.alerting.manager.api.parameter.ListAlertVO;
import org.bithon.server.alerting.manager.api.parameter.RuleFolderVO;
import org.bithon.server.alerting.manager.api.parameter.UpdateAlertRuleRequest;
import org.bithon.server.alerting.manager.biz.AlertCommandService;
import org.bithon.server.alerting.manager.biz.BizException;
import org.bithon.server.commons.json.JsonPayloadFormatter;
import org.bithon.server.storage.alerting.IAlertObjectStorage;
import org.bithon.server.storage.alerting.pojo.AlertChangeLogObject;
import org.bithon.server.storage.alerting.pojo.AlertStorageObject;
import org.bithon.server.storage.alerting.pojo.ListAlertDTO;
import org.bithon.server.storage.alerting.pojo.ListResult;
import org.bithon.server.storage.alerting.pojo.NotificationProps;
import org.bithon.server.storage.datasource.ISchema;
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
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/29
 */
@Slf4j
@CrossOrigin
@RestController
@Conditional(ManagerModuleEnabler.class)
public class AlertRuleApi {

    final IDataSourceApi dataSourceApi;
    final AlertCommandService commandService;
    final IAlertObjectStorage storage;
    final ObjectMapper objectMapper;

    public AlertRuleApi(AlertCommandService commandService,
                        IAlertObjectStorage storage,
                        IDataSourceApi dataSourceApi,
                        ObjectMapper objectMapper) {
        this.commandService = commandService;
        this.dataSourceApi = dataSourceApi;
        this.storage = storage;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/api/alerting/alert/create")
    public ApiResponse<String> createAlertRule(@Valid @RequestBody CreateAlertRuleRequest request) {
        try {
            AlertRule rule = Validator.validate(request).toAlert();
            return ApiResponse.success(commandService.createRule(rule, request.toCommandArgs()));
        } catch (BizException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/api/alerting/alert/update")
    public ApiResponse<?> updateAlertRule(@Valid @RequestBody UpdateAlertRuleRequest request) {
        Preconditions.checkNotNull(request.getId(), "id should not be null");
        try {
            AlertRule rule = Validator.validate(request).toAlert();
            rule.setEnabled(request.isEnabled());
            commandService.updateRule(rule);
            return ApiResponse.success();
        } catch (BizException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/api/alerting/alert/enable")
    public ApiResponse<?> enableRule(@Valid @RequestBody GenericAlertByIdRequest request) {
        try {
            commandService.enableRule(request.getAlertId());
            return ApiResponse.success();
        } catch (BizException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/api/alerting/alert/disable")
    public ApiResponse<Long> disableRule(@Valid @RequestBody GenericAlertByIdRequest request) {
        try {
            commandService.disableRule(request.getAlertId());
            return ApiResponse.success();
        } catch (BizException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/api/alerting/alert/delete")
    public ApiResponse<?> deleteRule(@Valid @RequestBody GenericAlertByIdRequest request) {
        try {
            commandService.deleteRule(request.getAlertId());
            return ApiResponse.success();
        } catch (BizException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    /**
     * @return evaluation log of this alert rule
     */
    @PostMapping("/api/alerting/alert/test")
    public ApiResponse<?> testRule(@Valid @RequestBody CreateAlertRuleRequest request) {
        try {
            AlertRule rule = Validator.validate(request).toAlert();
            rule.initialize();
            return ApiResponse.success(commandService.testRule(rule));
        } catch (BizException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }


    public static class AlertRuleVO extends AlertStorageObject {
        @Getter
        @Setter
        private Collection<AlertExpression> parsedExpressions;

        public AlertRuleVO(AlertStorageObject alertStorageObject, Collection<AlertExpression> parsedExpressions) {
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
    public ApiResponse<AlertRuleVO> getRuleById(@Valid @RequestBody GenericAlertByIdRequest request) {
        AlertStorageObject ruleObject = storage.getRuleById(request.getAlertId());
        if (ruleObject != null) {
            return ApiResponse.success(toVO(ruleObject));
        }
        return ApiResponse.fail("Alert rule not found");
    }

    @Data
    public static class GetRuleByFolderRequest {
        private String parentFolder;
    }

    @Data
    @Builder
    public static class GetRuleByFolderResponse {
        private List<AlertRuleVO> rules;
    }

    @PostMapping("/api/alerting/alert/folder/rules")
    public GetRuleByFolderResponse getRuleByFolder(@Valid @RequestBody GetRuleByFolderRequest request) {
        List<AlertStorageObject> ruleObject = storage.getRuleByFolder(request.getParentFolder());

        return GetRuleByFolderResponse.builder()
                                      .rules(ruleObject.stream()
                                                       .map(this::toVO)
                                                       .toList())
                                      .build();
    }

    private AlertRuleVO toVO(AlertStorageObject ruleObject) {
        IExpression alertExpression = AlertExpressionASTParser.parse(ruleObject.getPayload().getExpr());

        // Get Schema for validation
        Map<String, ISchema> schemas = dataSourceApi.getSchemas();

        // Flatten expressions
        List<AlertExpression> alertExpressions = new ArrayList<>();
        alertExpression.accept((IAlertInDepthExpressionVisitor) expression -> {
            expression.getMetricExpression().validate(schemas);
            alertExpressions.add(expression);
        });

        // Backward compatibility
        if (ruleObject.getPayload().getNotifications() != null && ruleObject.getPayload().getNotificationProps() == null) {
            ruleObject.getPayload().setNotificationProps(NotificationProps.builder()
                                                                          .renderExpressions(new TreeSet<>(alertExpressions.stream().map(AlertExpression::getId).toList()))
                                                                          .silence(ruleObject.getPayload().getSilence())
                                                                          .channels(ruleObject.getPayload().getNotifications())
                                                                          .build());
        }

        return new AlertRuleVO(ruleObject, alertExpressions);
    }

    @PostMapping("/api/alerting/alert/folder")
    public GetRuleFoldersResponse getFolders(@Valid @RequestBody GetRuleFoldersRequest request) {
        List<String> names = storage.getNames(request.getParentFolder());

        // Map to count the number of elements under each immediate subfolder
        // Use TreeMap to retain the order
        Map<String, Integer> folderCount = new TreeMap<>();
        for (String name : names) {
            int idx = name.lastIndexOf('/');
            String folder = idx == -1 ? "<ROOT>" : name.substring(0, idx);
            folderCount.compute(folder, (k, v) -> v == null ? 1 : v + 1);
        }

        List<RuleFolderVO> folders = folderCount.entrySet()
                                                .stream()
                                                .map(e -> {
                                                    RuleFolderVO vo = new RuleFolderVO();
                                                    vo.setFolder(e.getKey());
                                                    vo.setRuleCount(e.getValue());
                                                    return vo;
                                                })
                                                .collect(Collectors.toList());

        return GetRuleFoldersResponse.builder()
                                     .folders(folders)
                                     .build();
    }

    @PostMapping("/api/alerting/alert/list")
    public GetAlertListResponse getRuleList(@Valid @RequestBody GetAlertListRequest request) {
        List<ListAlertDTO> alertList = storage.getAlertList(request.getFolder(),
                                                            request.getAppName(),
                                                            request.getAlertName(),
                                                            request.getOrderBy(),
                                                            request.getLimit());

        return new GetAlertListResponse(storage.getAlertListSize(request.getAppName(), request.getAlertName()),
                                        alertList.stream()
                                                 .map(alert -> {
                                                     ListAlertVO vo = new ListAlertVO();
                                                     vo.setAlertId(alert.getAlertId());
                                                     vo.setName(alert.getAlertName());
                                                     vo.setAppName(alert.getAppName());
                                                     vo.setEnabled(!alert.isDisabled());
                                                     vo.setCreatedAt(alert.getCreatedAt().getTime());
                                                     vo.setUpdatedAt(alert.getUpdatedAt().getTime());
                                                     vo.setLastEvaluatedAt(alert.getLastEvaluatedAt() == null ? 0 : alert.getLastEvaluatedAt().getTime());
                                                     vo.setLastAlertAt(alert.getLastAlertAt() == null ? 0L : alert.getLastAlertAt().getTime());
                                                     vo.setLastOperator(alert.getLastOperator());
                                                     vo.setLastRecordId(alert.getLastRecordId());
                                                     vo.setAlertStatus(alert.getAlertStatus());
                                                     return vo;
                                                 })
                                                 .collect(Collectors.toList()));
    }

    @PostMapping("/api/alerting/alert/change-log/get")
    public GetChangeLogListResponse getChangeLogs(@Valid @RequestBody GetAlertChangeLogListRequest request) {
        ListResult<AlertChangeLogObject> results = storage.getChangeLogs(request.getAlertId(),
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
}
