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

package org.bithon.server.alerting.notification.provider.ding;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.time.DateTime;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.evaluator.result.EvaluationResult;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.common.utils.FreeMarkerUtil;
import org.bithon.server.alerting.notification.format.NotificationContent;
import org.bithon.server.alerting.notification.format.NotificationTextSection;
import org.bithon.server.alerting.notification.format.QuotedTextLine;
import org.bithon.server.alerting.notification.format.TextLine;
import org.bithon.server.alerting.notification.message.ConditionEvaluationResult;
import org.bithon.server.alerting.notification.message.DimensionConditionTextManager;
import org.bithon.server.alerting.notification.message.NotificationMessage;
import org.bithon.server.alerting.notification.message.OutputMessage;
import org.bithon.server.alerting.notification.provider.INotificationProvider;
import org.bithon.server.commons.time.TimeSpan;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/11/30 6:53 下午
 */
@Slf4j
public class DingNotificationProvider implements INotificationProvider {
    @Getter
    @NotEmpty
    private final String url;

    private final DingNotificationConfig config;
    private final DimensionConditionTextManager dimensionConditionTextManager;

    @Getter
    @Setter
    @JsonIgnore
    private String name;

    @JsonCreator
    public DingNotificationProvider(@JsonProperty("url") @Nullable String url,
                                    @JacksonInject DingNotificationConfig config,
                                    @JacksonInject DimensionConditionTextManager conditionTextManager) {
        this.url = url;
        this.config = config;
        this.dimensionConditionTextManager = conditionTextManager;
    }

    @Override
    public void notify(NotificationMessage message) throws Exception {
        AlertRule alertRule = message.getAlertRule();

        long alertAt = System.currentTimeMillis();

        String title = StringUtils.format("[%s]Application Alert", StringUtils.format("MM-dd HH:mm:ss", alertAt));
        NotificationContent notificationContent = new NotificationContent();
        NotificationTextSection section = notificationContent.getDefaultSection();
        section.add("Name", alertRule.getName())
               .add("Time Window",
                    StringUtils.format("%s ~ %s",
                                       DateTime.formatDateTime("MM-dd HH:mm",
                                                               new TimeSpan(message.getEnd()).before(alertRule.getForDuration())
                                                                                             .getMilliseconds()),
                                       DateTime.formatDateTime("MM-dd HH:mm", message.getEnd())));

        if (message.getLastAlertAt() != null) {
            section.add("Last alert at", StringUtils.format("MM-dd HH:mm:ss", message.getLastAlertAt()));
        }
        section.add("Alert at", StringUtils.format("MM-dd HH:mm:ss", alertAt));

        for (AlertExpression expression : message.getExpressions()) {
            ConditionEvaluationResult result = message.getConditionEvaluation().get(expression.getId());
            if (result == null || result.getResult() != EvaluationResult.MATCHED || result.getOutputs() != null) {
                continue;
            }

            StringBuilder text = new StringBuilder("Expression");
            text.append(expression.getId());
            text.append(StringUtils.format(": %d minutes", alertRule.getForDuration()));
            text.append(expression.getWhere());

            OutputMessage output = result.getOutputs();
            text.append(StringUtils.format("%s(%s.%s), Now [%s], Incremental [%s]\n",
                                           expression.getSelect().getAggregator(),
                                           expression.getFrom(),
                                           expression.getSelect().getName(),
                                           output.getCurrent(),
                                           output.getDelta()));

            section.add(new QuotedTextLine(text.toString()));
            Future<String> url = message.getImages().get(expression.getId());
            if (url == null) {
                continue;
            }

            try {
                String imageLink = url.get(2, TimeUnit.SECONDS);
                if (imageLink != null) {
                    section.add(new TextLine(StringUtils.format("![Metric](%s)", imageLink)));
                }
            } catch (Exception e) {
                //context.logException(DingNotificationProvider.class, e, "Exception");
            }
        }
        if (config.getRecordLinkTemplate() != null) {
            section.add(new TextLine(StringUtils.format("[View Detail](" + config.getRecordLinkTemplate() + ")", message.getAlertRecordId())));
        }

        String content = FreeMarkerUtil.applyTemplate("/templates/alarm-notification.ftl", notificationContent);

        DingMessage.builder()
                   .content(content)
                   .createdTime(alertAt)
                   .applicationName(alertRule.getAppName())
                   .header(title)
                   .title(title)
                   .build();
    }

    @Override
    public String toString() {
        return "DingNotificationProvider{" +
            "url='" + url + '\'' +
            '}';
    }
}
