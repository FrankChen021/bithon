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

package org.bithon.server.alerting.notification.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.evaluator.result.EvaluationOutput;
import org.bithon.server.alerting.common.evaluator.result.EvaluationOutputs;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.notification.NotificationModuleEnabler;
import org.bithon.server.alerting.notification.channel.INotificationChannel;
import org.bithon.server.alerting.notification.channel.NotificationChannelFactory;
import org.bithon.server.alerting.notification.image.AlertImageRenderService;
import org.bithon.server.alerting.notification.image.ImageMode;
import org.bithon.server.alerting.notification.message.NotificationMessage;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.alerting.IAlertNotificationChannelStorage;
import org.bithon.server.storage.alerting.pojo.AlertStatus;
import org.bithon.server.storage.alerting.pojo.NotificationChannelObject;
import org.springframework.context.annotation.Conditional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/12/22 17:02
 */
@Slf4j
@RestController
@Conditional(NotificationModuleEnabler.class)
public class NotificationApiImpl implements INotificationApi {

    private final IAlertNotificationChannelStorage channelStorage;
    private Map<String, INotificationChannel> channels = new HashMap<>();
    private final AlertImageRenderService imageService;
    private final ObjectMapper objectMapper;

    public NotificationApiImpl(AlertImageRenderService imageService,
                               IAlertNotificationChannelStorage channelStorage,
                               ObjectMapper objectMapper) {
        this.imageService = imageService;
        this.objectMapper = objectMapper;
        this.channelStorage = channelStorage;
    }

    @Scheduled(cron = "3 0/1 * 1/1 * ?")
    public void loadChannels() {
        // TODO: Change to incremental loading
        log.info("Loading notification channels...");
        Map<String, INotificationChannel> newChannels = new HashMap<>();
        channelStorage.getChannels(IAlertNotificationChannelStorage.GetChannelRequest.builder().since(0).build())
                      .forEach((channelStorageObject) -> {
                          try {
                              toChannel(channelStorageObject);
                          } catch (IOException e) {
                              throw new RuntimeException(e);
                          }
                      });
        this.channels = newChannels;
    }

    @Override
    public void notify(String name, NotificationMessage message) throws Exception {
        if (imageService.isEnabled()
            // ONLY render image for ALERTING message this is because RESOLVED message is a bit tricky
            && message.getStatus() == AlertStatus.ALERTING) {

            // Find out evaluated expressions
            List<AlertImageRenderService.EvaluatedExpression> expressionList =
                message.getEvaluationOutputs()
                       .entrySet()
                       .stream()
                       .map(entry -> {
                           String expressionId = entry.getKey();
                           EvaluationOutputs outputs = entry.getValue();
                           AlertExpression expression = message.getExpressions()
                                                               .values()
                                                               .stream()
                                                               .filter((expr) -> expr.getId().equals(expressionId))
                                                               .findFirst()
                                                               .orElse(null);
                           if (expression == null) {
                               return null;
                           }
                           return AlertImageRenderService.EvaluatedExpression.builder()
                                                                             .alertExpression(expression)
                                                                             .labels(outputs.stream()
                                                                                            .map(EvaluationOutput::getLabel)
                                                                                            .filter(label -> !label.isEmpty())
                                                                                            .toList())
                                                                             .build();
                       })
                       .filter(Objects::nonNull)
                       .toList();

            TimeSpan endSpan = TimeSpan.of(message.getEndTimestamp());

            Map<String, String> images = this.imageService.render(ImageMode.BASE64,
                                                                  message.getAlertRule(),
                                                                  expressionList,
                                                                  endSpan);
            message.setImages(images);
        }

        INotificationChannel channel = channels.get(name);
        if (channel == null) {
            // Try to load the channel from storage
            NotificationChannelObject channelStorageObject = this.channelStorage.getChannel(name);
            if (channelStorageObject != null) {
                channel = toChannel(channelStorageObject);
            } else {
                throw new RuntimeException(StringUtils.format("Channel [%s] not found.", name));
            }
        }
        channel.send(message);
    }

    protected INotificationChannel toChannel(NotificationChannelObject storageObject) throws IOException {
        INotificationChannel channel = NotificationChannelFactory.create(storageObject.getType(),
                                                                         storageObject.getName(),
                                                                         storageObject.getPayload(),
                                                                         this.objectMapper);
        channels.put(storageObject.getName(), channel);
        return channel;
    }
}
