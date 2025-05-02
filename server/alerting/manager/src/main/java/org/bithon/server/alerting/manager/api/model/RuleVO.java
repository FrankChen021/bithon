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

package org.bithon.server.alerting.manager.api.model;


import lombok.Getter;
import lombok.Setter;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.model.IAlertInDepthExpressionVisitor;
import org.bithon.server.alerting.common.parser.AlertExpressionASTParser;
import org.bithon.server.storage.alerting.pojo.AlertStorageObject;
import org.bithon.server.storage.alerting.pojo.NotificationProps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * @author frank.chen021@outlook.com
 * @date 2/5/25 11:38 am
 */
public class RuleVO extends AlertStorageObject {
    @Getter
    @Setter
    private Collection<AlertExpression> parsedExpressions;

    public RuleVO(AlertStorageObject alertStorageObject, Collection<AlertExpression> parsedExpressions) {
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

    public static RuleVO from(AlertStorageObject rule) {
        IExpression alertExpression = AlertExpressionASTParser.parse(rule.getPayload().getExpr());

        // Flatten expressions
        List<AlertExpression> flattenExpressions = new ArrayList<>();
        alertExpression.accept((IAlertInDepthExpressionVisitor) flattenExpressions::add);

        // Backward compatibility
        if (rule.getPayload().getNotifications() != null && rule.getPayload().getNotificationProps() == null) {
            rule.getPayload().setNotificationProps(NotificationProps.builder()
                                                                    .renderExpressions(new TreeSet<>(flattenExpressions.stream().map(AlertExpression::getId).toList()))
                                                                    .silence(rule.getPayload().getSilence())
                                                                    .channels(rule.getPayload().getNotifications())
                                                                    .build());
        }

        return new RuleVO(rule, flattenExpressions);
    }
}
