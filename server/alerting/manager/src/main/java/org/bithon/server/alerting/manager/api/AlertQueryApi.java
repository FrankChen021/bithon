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

import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.manager.ManagerModuleEnabler;
import org.bithon.server.alerting.manager.api.parameter.ApiResponse;
import org.bithon.server.alerting.manager.api.parameter.ChangeLogBo;
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
import org.bithon.server.alerting.manager.api.parameter.ListAlertBo;
import org.bithon.server.alerting.manager.api.parameter.ListRecordBo;
import org.bithon.server.alerting.manager.biz.EvaluationLogService;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.alerting.IAlertObjectStorage;
import org.bithon.server.storage.alerting.IAlertRecordStorage;
import org.bithon.server.storage.alerting.pojo.AlertChangeLogObject;
import org.bithon.server.storage.alerting.pojo.AlertRecordObject;
import org.bithon.server.storage.alerting.pojo.AlertStorageObject;
import org.bithon.server.storage.alerting.pojo.ListAlertDO;
import org.bithon.server.storage.alerting.pojo.ListResult;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
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

    public AlertQueryApi(IAlertRecordStorage alertRecordStorage, IAlertObjectStorage alertStorage, EvaluationLogService evaluationLogService) {
        this.alertRecordStorage = alertRecordStorage;
        this.alertStorage = alertStorage;
        this.evaluationLogService = evaluationLogService;
    }

    @PostMapping("/api/alerting/alert/get")
    public ApiResponse<AlertStorageObject> getAlertById(@Valid @RequestBody GenericAlertByIdRequest request) {
        return ApiResponse.success(alertStorage.getAlertById(request.getAlertId()));
    }

    @PostMapping("/api/alerting/alert/list")
    public GetAlertListResponse getAlerts(@Valid @RequestBody GetAlertListRequest request) {
        request.getOrderBy().setName(StringUtils.camelToSnake(request.getOrderBy().getName()));

        List<ListAlertDO> objs = alertStorage.getAlertList(request.getAppName(),
                                                           request.getAlertName(),
                                                           request.getOrderBy(),
                                                           request.getLimit());

        return new GetAlertListResponse(alertStorage.getAlertListSize(request.getAppName(), request.getAlertName()),
                                        objs.stream()
                                            .map(alert -> {
                                                ListAlertBo bo = new ListAlertBo();
                                                bo.setAlertId(alert.getAlertId());
                                                bo.setName(alert.getAlertName());
                                                bo.setAppName(alert.getAppName());
                                                bo.setEnabled(!alert.isDisabled());
                                                bo.setCreatedAt(alert.getCreatedAt().getTime());
                                                bo.setUpdatedAt(alert.getUpdatedAt().getTime());
                                                bo.setLastAlertAt(alert.getLastAlertAt() == null ? 0L : alert.getLastAlertAt().getTime());
                                                bo.setLastOperator(alert.getLastOperator());
                                                bo.setLastRecordId(alert.getLastRecordId());
                                                return bo;
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
        ListResult<AlertRecordObject> results = alertRecordStorage.getAlertRecords(request.getAlertId(), request.getPageNumber(), request.getPageSize());
        return new GetAlertRecordListResponse(results.getRows(),
                                              results.getData()
                                                     .stream()
                                                     .map(obj -> {
                                                         ListRecordBo bo = new ListRecordBo();
                                                         bo.setAlarmName(obj.getAlertName());
                                                         bo.setAlertId(obj.getAlertId());
                                                         bo.setAppName(obj.getAppName());
                                                         bo.setEnv(obj.getNamespace());
                                                         bo.setId(obj.getRecordId());
                                                         bo.setServerCreateTime(obj.getCreatedAt());
                                                         return bo;
                                                     }).collect(Collectors.toList()));
    }

    @PostMapping("/api/alerting/alert/change-log/get")
    public GetChangeLogListResponse getChangeLogs(@Valid @RequestBody GetAlertChangeLogListRequest request) {
        ListResult<AlertChangeLogObject> results = alertStorage.getChangeLogs(request.getAlertId(), request.getPageNumber(), request.getPageSize());
        return new GetChangeLogListResponse(results.getRows(),
                                            results.getData()
                                                   .stream()
                                                   .map(log -> {
                                                       ChangeLogBo bo = new ChangeLogBo();
                                                       BeanUtils.copyProperties(log, bo);
                                                       bo.setTimestamp(log.getCreatedAt().getTime());
                                                       return bo;
                                                   }).collect(Collectors.toList()));
    }

    @PostMapping("/api/alerting/alert/evaluation-log/get")
    public GetEvaluationLogsResponse getEvaluationLogs(@Valid @RequestBody GetEvaluationLogsRequest request) {
        return this.evaluationLogService.getEvaluationLogs(request.getAlertId(),
                                                           TimeSpan.fromISO8601(request.getInterval().getStartISO8601()),
                                                           TimeSpan.fromISO8601(request.getInterval().getEndISO8601()));
    }
}
