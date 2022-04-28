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

package org.bithon.server.alerting.admin.api;

import lombok.Data;
import org.bithon.server.alerting.admin.biz.CommandArgs;
import org.bithon.server.alerting.common.model.Alert;
import org.bithon.server.alerting.common.model.AlertCompositeConditions;
import org.bithon.server.alerting.common.model.AlertCondition;
import org.bithon.server.alerting.common.notification.provider.INotificationProvider;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/31
 */
@Data
public class CreateAlertRequest {

    /**
     * check if target application exists before create an alert for that application
     */
    private boolean checkApplication = true;

    /**
     * Optional.
     * Unique id of alert object
     */
    private String id;

    @NotEmpty
    private String appName;

    @NotEmpty
    private String name;

    /**
     * in minute
     */
    @Min(1)
    @Max(60)
    private int evaluationInterval = 1;

    @Min(1)
    @Max(60)
    private int matchTimes = 3;

    @Valid
    @Size(min = 1)
    private List<AlertCondition> conditions;

    @Valid
    @Size(min = 1)
    private List<AlertCompositeConditions> rules;

    @Valid
    @NotNull
    private INotificationProvider[] notifications;

    private boolean disabled = false;

    public Alert toAlert() {
        Alert alert = new Alert();
        alert.setId(this.getId());
        alert.setAppName(this.getAppName());
        alert.setConditions(this.getConditions());
        alert.setName(this.getName());
        alert.setNotifications(this.getNotifications());
        alert.setRules(this.getRules());
        alert.setMatchTimes(this.getMatchTimes());
        alert.setEnabled(!disabled);
        return alert;
    }

    public CommandArgs toCommandArgs() {
        return CommandArgs.builder().checkApplicationExist(checkApplication).build();
    }
}
