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

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.server.alerting.common.model.IAlertInDepthExpressionVisitor;
import org.bithon.server.alerting.common.parser.AlertExpressionASTParser;
import org.bithon.server.storage.alerting.pojo.AlertStatus;
import org.bithon.server.storage.alerting.pojo.ListRuleDTO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Note: the field name here must be the camel cases of the names in the database,
 * or the frontend sort requests might fail
 *
 * @author frank.chen021@outlook.com
 * @date 2021/1/5
 */
@Data
public class RuleListItemVO {
    private String id;
    private String appName;
    private String name;
    private boolean disabled;

    private String lastOperator;
    private long createdAt;
    private long updatedAt;

    private Collection<IExpression> parsedExpressions;

    // Runtime properties
    private long lastEvaluatedAt;
    private Long lastAlertedAt;
    private String lastRecordId;
    private AlertStatus alertStatus;

    public static RuleListItemVO from(ListRuleDTO rule) {
        return from(rule, false);
    }

    public static RuleListItemVO from(ListRuleDTO rule, boolean parseExpressions) {
        RuleListItemVO vo = new RuleListItemVO();
        vo.setId(rule.getId());
        vo.setName(rule.getName());
        vo.setAppName(rule.getAppName());
        vo.setDisabled(rule.isDisabled());
        vo.setCreatedAt(rule.getCreatedAt().getTime());
        vo.setUpdatedAt(rule.getUpdatedAt().getTime());
        vo.setLastEvaluatedAt(rule.getLastEvaluatedAt() == null ? 0 : rule.getLastEvaluatedAt().getTime());
        vo.setLastAlertedAt(rule.getLastAlertAt() == null ? 0L : rule.getLastAlertAt().getTime());
        vo.setLastOperator(rule.getLastOperator());
        vo.setLastRecordId(rule.getLastRecordId());
        vo.setAlertStatus(rule.getAlertStatus());

        if (parseExpressions) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode payload = objectMapper.readTree(rule.getPayload());
                String expressionText = payload.get("expr").asText();

                IExpression alertExpression = AlertExpressionASTParser.parse(expressionText);

                // Flatten expressions
                List<IExpression> flattenExpressions = new ArrayList<>();
                alertExpression.accept((IAlertInDepthExpressionVisitor) flattenExpressions::add);

                vo.setParsedExpressions(flattenExpressions);
            } catch (JacksonException ignored) {
            }
        }

        return vo;
    }
}
