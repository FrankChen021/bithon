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
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.common.utils.Validator;
import org.bithon.server.alerting.manager.ManagerModuleEnabler;
import org.bithon.server.alerting.manager.api.model.ApiResponse;
import org.bithon.server.alerting.manager.api.model.ChangeLogVO;
import org.bithon.server.alerting.manager.api.model.CreateRuleRequest;
import org.bithon.server.alerting.manager.api.model.GenericAlertByIdRequest;
import org.bithon.server.alerting.manager.api.model.GetChangeLogListRequest;
import org.bithon.server.alerting.manager.api.model.GetChangeLogListResponse;
import org.bithon.server.alerting.manager.api.model.GetRuleFoldersRequest;
import org.bithon.server.alerting.manager.api.model.GetRuleFoldersResponse;
import org.bithon.server.alerting.manager.api.model.GetRuleListRequest;
import org.bithon.server.alerting.manager.api.model.GetRuleListResponse;
import org.bithon.server.alerting.manager.api.model.RuleFolderVO;
import org.bithon.server.alerting.manager.api.model.RuleListItemVO;
import org.bithon.server.alerting.manager.api.model.RuleVO;
import org.bithon.server.alerting.manager.api.model.UpdateRuleRequest;
import org.bithon.server.alerting.manager.biz.AlertCommandService;
import org.bithon.server.alerting.manager.biz.BizException;
import org.bithon.server.commons.json.JsonPayloadFormatter;
import org.bithon.server.datasource.query.OrderBy;
import org.bithon.server.storage.alerting.IAlertObjectStorage;
import org.bithon.server.storage.alerting.pojo.AlertChangeLogObject;
import org.bithon.server.storage.alerting.pojo.AlertStorageObject;
import org.bithon.server.storage.alerting.pojo.ListResult;
import org.bithon.server.storage.alerting.pojo.ListRuleDTO;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
    public ApiResponse<String> createAlertRule(@Valid @RequestBody CreateRuleRequest request) {
        try {
            AlertRule rule = Validator.validate(request).toAlert();
            return ApiResponse.success(commandService.createRule(rule, request.toCommandArgs()));
        } catch (BizException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/api/alerting/alert/update")
    public ApiResponse<?> updateAlertRule(@Valid @RequestBody UpdateRuleRequest request) {
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
    public ApiResponse<?> testRule(@Valid @RequestBody CreateRuleRequest request) {
        try {
            AlertRule rule = Validator.validate(request).toAlert();
            rule.initialize();
            return ApiResponse.success(commandService.testRule(rule));
        } catch (BizException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/api/alerting/alert/get")
    public ApiResponse<RuleVO> getRuleById(@Valid @RequestBody GenericAlertByIdRequest request) {
        AlertStorageObject rule = storage.getRuleById(request.getAlertId());
        if (rule != null) {
            return ApiResponse.success(RuleVO.from(rule));
        }
        return ApiResponse.fail("Alert rule not found");
    }

    /**
     * Get folders
     */
    @PostMapping("/api/alerting/alert/folder")
    public GetRuleFoldersResponse getFolders(@Valid @RequestBody GetRuleFoldersRequest request) {
        List<ListRuleDTO> rules = storage.getRuleList(request.getParentFolder(), null, null, null, null);

        // Map to count the number of elements under each immediate subfolder
        // Use TreeMap to retain the order
        Map<String, RuleFolderVO> folders = new TreeMap<>();
        for (ListRuleDTO rule : rules) {
            int idx = rule.getName().lastIndexOf('/');
            String folder = idx == -1 ? "<ROOT>" : rule.getName().substring(0, idx);
            RuleFolderVO vo = folders.computeIfAbsent(folder, (k) -> {
                RuleFolderVO voObject = new RuleFolderVO();
                voObject.setFolder(k);
                return voObject;
            });
            vo.updateCount();
            vo.updateLastAlertedAt(rule.getLastAlertAt());
            vo.updateLastUpdatedAt(rule.getUpdatedAt());
            vo.updateLastEvaluatedAt(rule.getLastEvaluatedAt());
        }

        List<RuleFolderVO> folderList = folders.values()
                                               .stream()
                                               .sorted(Comparator.comparing(RuleFolderVO::getFolder))
                                               .toList();

        return GetRuleFoldersResponse.builder()
                                     .folders(folderList)
                                     .build();
    }

    @PostMapping("/api/alerting/alert/list")
    public GetRuleListResponse getRuleList(@Valid @RequestBody GetRuleListRequest request) {
        List<ListRuleDTO> alertList = storage.getRuleList(request.getFolder(),
                                                          request.getAppName(),
                                                          request.getAlertName(),
                                                          request.getOrderBy(),
                                                          request.getLimit());

        return new GetRuleListResponse(storage.getRuleListSize(request.getAppName(), request.getAlertName()),
                                       alertList.stream()
                                                 .map(RuleListItemVO::from)
                                                 .collect(Collectors.toList()));
    }

    @Data
    public static class GetRuleByFolderRequest {
        private String parentFolder;
        private OrderBy orderBy;
    }

    @Data
    @Builder
    public static class GetRuleByFolderResponse {
        private List<RuleListItemVO> rules;
    }

    @PostMapping("/api/alerting/alert/folder/rules")
    public GetRuleByFolderResponse getRuleByFolder(@Valid @RequestBody GetRuleByFolderRequest request) {
        List<ListRuleDTO> ruleObject = storage.getRuleList(request.getParentFolder() == null ? "" : request.getParentFolder().trim(),
                                                           null,
                                                           null,
                                                           request.getOrderBy(),
                                                           null);

        return GetRuleByFolderResponse.builder()
                                      .rules(ruleObject.stream()
                                                       .map((rule) -> RuleListItemVO.from(rule, true))
                                                       .toList())
                                      .build();
    }

    @PostMapping("/api/alerting/alert/change-log/get")
    public GetChangeLogListResponse getChangeLogs(@Valid @RequestBody GetChangeLogListRequest request) {
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
