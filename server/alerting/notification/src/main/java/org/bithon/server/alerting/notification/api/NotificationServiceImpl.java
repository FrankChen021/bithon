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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.alerting.notification.NotificationModuleEnabler;
import org.bithon.server.alerting.notification.image.AlertImageRenderService;
import org.bithon.server.alerting.notification.message.NotificationMessage;
import org.bithon.server.alerting.notification.provider.INotificationProvider;
import org.bithon.server.storage.alerting.IAlertNotificationProviderStorage;
import org.springframework.context.annotation.Conditional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/12/22 17:02
 */
@Slf4j
@RestController
@Conditional(NotificationModuleEnabler.class)
public class NotificationServiceImpl implements INotificationApi {

    private final IAlertNotificationProviderStorage notificationProviderStorage;
    private Map<String, INotificationProvider> providers = new HashMap<>();
    private final AlertImageRenderService imageService;
    private final ObjectMapper objectMapper;

    public NotificationServiceImpl(AlertImageRenderService imageService,
                                   IAlertNotificationProviderStorage notificationProviderStorage, ObjectMapper objectMapper) {
        this.imageService = imageService;
        this.objectMapper = objectMapper;
        this.notificationProviderStorage = notificationProviderStorage;
    }

    @Scheduled(cron = "3 0/1 * 1/1 * ?")
    public void loadProviders() {
        log.info("Loading notification providers...");
        Map<String, INotificationProvider> newProviders = new HashMap<>();
        notificationProviderStorage.loadProviders(0)
                                   .forEach((provider) -> {
                   try {
                       newProviders.put(provider.getProviderId(), objectMapper.readValue(provider.getPayload(), INotificationProvider.class));
                   } catch (JsonProcessingException e) {
                       throw new RuntimeException(e);
                   }
               });
        this.providers = newProviders;
    }

    @Override
    public void notify(String providerId, NotificationMessage message) throws Exception {
        if (imageService.isEnabled()) {
            /*
            message.setImages(new HashMap<>());
            message.getConditionEvaluation().forEach((id, result) -> {
                if (result.getResult() == EvaluationResult.MATCHED) {
                    message.getImages().put(id, this.imageService.renderAndSaveAsync(message.getNotifications().getImageMode(),
                                                                                     message.getAlert().getName(),
                                                                                     condition,
                                                                                     alert.getMatchTimes(),
                                                                                     context.getIntervalEnd()
                                                                                            .before(condition.getMetric().getWindow(), TimeUnit.MINUTES),
                                                                                     context.getIntervalEnd()))
                }
            });*/
        }

        INotificationProvider provider = providers.get(providerId);
        if (provider != null) {
            provider.notify(message);
        }
    }
}
