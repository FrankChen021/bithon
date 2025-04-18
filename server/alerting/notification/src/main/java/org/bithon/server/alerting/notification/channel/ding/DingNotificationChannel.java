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

package org.bithon.server.alerting.notification.channel.ding;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.evaluator.result.EvaluationOutput;
import org.bithon.server.alerting.common.evaluator.result.EvaluationOutputs;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.common.utils.Validator;
import org.bithon.server.alerting.notification.channel.INotificationChannel;
import org.bithon.server.alerting.notification.message.NotificationMessage;
import org.bithon.server.alerting.notification.message.format.NotificationContent;
import org.bithon.server.alerting.notification.message.format.NotificationTextSection;
import org.bithon.server.alerting.notification.message.format.QuotedTextLine;
import org.bithon.server.alerting.notification.message.format.TextLine;

import java.time.Duration;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/11/30 6:53 下午
 */
@Slf4j
public class DingNotificationChannel implements INotificationChannel {
    @Data
    public static class Props {
        @NotEmpty
        private final String url;
    }

    private final Props props;
    private final DingNotificationConfig config;

    @JsonCreator
    public DingNotificationChannel(@JsonProperty("props") Props props,
                                   @JacksonInject DingNotificationConfig config) {
        this.props = Preconditions.checkNotNull(props, "props property can not be null");
        Validator.validate(props);
        this.config = config;
    }

    @Override
    public void send(NotificationMessage message) throws Exception {
        AlertRule alertRule = message.getAlertRule();

        long alertAt = System.currentTimeMillis();

        String title = StringUtils.format("[%s]Application Alert", StringUtils.format("MM-dd HH:mm:ss", alertAt));
        NotificationContent notificationContent = new NotificationContent();
        NotificationTextSection section = notificationContent.getDefaultSection();
        section.add("Name", alertRule.getName());
        if (message.getLastAlertAt() != null) {
            section.add("Last alert at", StringUtils.format("MM-dd HH:mm:ss", message.getLastAlertAt()));
        }
        section.add("Alert at", StringUtils.format("MM-dd HH:mm:ss", alertAt));

        for (AlertExpression expression : message.getExpressions().values()) {
            EvaluationOutputs outputs = message.getEvaluationOutputs().get(expression.getId());
            if (outputs == null || !outputs.isMatched()) {
                continue;
            }

            StringBuilder text = new StringBuilder("Expression");
            text.append(expression.getId());
            text.append(expression.serializeToText());

            for (EvaluationOutput output : outputs) {
                text.append(StringUtils.format("%s(%s.%s), Now [%s], Incremental [%s]\n",
                                               expression.getMetricExpression().getMetric().getAggregator(),
                                               expression.getMetricExpression().getFrom(),
                                               expression.getMetricExpression().getMetric().getName(),
                                               output.getCurrent(),
                                               output.getDelta()));
            }

            section.add(new QuotedTextLine(text.toString()));
            /*
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
            */
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
    public void test(NotificationMessage message, Duration timeout) throws Exception {
        send(message);
    }

    @Override
    public String toString() {
        return "DingNotificationChannel{" +
               "url='" + this.props.url + '\'' +
               '}';
    }
}
