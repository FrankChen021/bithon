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
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.alerting.common.model.Alert;
import org.bithon.server.alerting.common.utils.Validator;
import org.bithon.server.alerting.manager.ManagerModuleEnabler;
import org.bithon.server.alerting.manager.api.parameter.ApiResponse;
import org.bithon.server.alerting.manager.api.parameter.CreateAlertRequest;
import org.bithon.server.alerting.manager.api.parameter.GenericAlertByIdRequest;
import org.bithon.server.alerting.manager.biz.AlertCommandService;
import org.bithon.server.alerting.manager.biz.BizException;
import org.bithon.server.storage.alerting.IAlertObjectStorage;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/29
 */
@Slf4j
@CrossOrigin
@RestController
@Conditional(ManagerModuleEnabler.class)
public class AlertCommandApi {

    final AlertCommandService commandService;
    final IAlertObjectStorage dao;
    final ObjectMapper om;

    public AlertCommandApi(AlertCommandService commandService, IAlertObjectStorage dao, ObjectMapper om) {
        this.commandService = commandService;
        this.dao = dao;
        this.om = om;
    }

    @PostMapping("/alerting/api/alert/create")
    public ApiResponse<String> createAlert(@Valid @RequestBody CreateAlertRequest request) {
        try {
            return ApiResponse.success(commandService.createAlert((Alert) Validator.validate(request.toAlert()),
                                                                  request.toCommandArgs()));
        } catch (BizException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/alerting/api/alert/update")
    public ApiResponse<?> updateAlert(@Valid @RequestBody CreateAlertRequest request) {
        Preconditions.checkNotNull(request.getId(), "id should not be null");
        try {
            commandService.updateAlert((Alert) Validator.validate(request.toAlert()));
            return ApiResponse.success();
        } catch (BizException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/alerting/api/alert/enable")
    public ApiResponse<?> enable(@Valid @RequestBody GenericAlertByIdRequest request) {
        try {
            commandService.enableAlert(request.getAlertId());
            return ApiResponse.success();
        } catch (BizException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/alerting/api/alert/disable")
    public ApiResponse<Long> disable(@Valid @RequestBody GenericAlertByIdRequest request) {
        try {
            commandService.disableAlert(request.getAlertId());
            return ApiResponse.success();
        } catch (BizException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/alerting/api/alert/delete")
    public ApiResponse<?> deleteAlertById(@Valid @RequestBody GenericAlertByIdRequest request) {
        try {
            commandService.deleteAlert(request.getAlertId());
            return ApiResponse.success();
        } catch (BizException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
