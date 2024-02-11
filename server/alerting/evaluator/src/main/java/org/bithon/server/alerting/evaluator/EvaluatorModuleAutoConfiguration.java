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

package org.bithon.server.alerting.evaluator;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import feign.Contract;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.evaluator.rpc.NotificationServiceClientApi;
import org.bithon.server.alerting.evaluator.storage.local.AlertStateLocalMemoryStorage;
import org.bithon.server.alerting.evaluator.storage.redis.AlertStateRedisStorage;
import org.bithon.server.alerting.notification.api.INotificationApi;
import org.bithon.server.discovery.client.DiscoveredServiceInvoker;
import org.bithon.server.storage.alerting.AlertingStorageConfiguration;
import org.bithon.server.storage.alerting.IAlertStateStorage;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.util.HashMap;

/**
 * @author Frank Chen
 * @date 12/11/21 6:18 pm
 */
@Configuration
@Conditional(EvaluatorModuleEnabler.class)
@ImportAutoConfiguration(value = {AlertingStorageConfiguration.class})
@EnableFeignClients
@Import(FeignClientsConfiguration.class)
public class EvaluatorModuleAutoConfiguration {

    @Bean
    public IAlertStateStorage alertStateStorage(ObjectMapper objectMapper,
                                                Environment environment) throws IOException {
        HashMap<?, ?> stateConfig = Binder.get(environment)
                                          .bind("bithon.alerting.evaluator.state", HashMap.class)
                                          .orElseGet(() -> null);
        Preconditions.checkIfTrue(stateConfig.containsKey("type"), "Missed 'type' property for bithon.alerting.evaluator.state");

        String jsonType = objectMapper.writeValueAsString(stateConfig);
        try {
            return objectMapper.readValue(jsonType, IAlertStateStorage.class);
        } catch (InvalidTypeIdException e) {
            throw new RuntimeException("Not found state storage with type " + stateConfig.get("type"));
        }
    }

    @Bean
    public NotificationServiceClientApi alertNotificationService(DiscoveredServiceInvoker discoveredServiceInvoker,
                                                                 Contract contract,
                                                                 Encoder encoder,
                                                                 Decoder decoder,
                                                                 Environment environment) {

        String service = environment.getProperty("bithon.alerting.evaluator.notification-service", "discovery");
        if ("discovery".equalsIgnoreCase(service)) {
            return new NotificationServiceClientApi(discoveredServiceInvoker.createUnicastApi(INotificationApi.class));
        }

        if (service.startsWith("http:") || service.startsWith("https:")) {
            return new NotificationServiceClientApi(Feign.builder()
                                                         .contract(contract)
                                                         .encoder(encoder)
                                                         .decoder(decoder)
                                                         .target(INotificationApi.class, service));
        }

        throw new RuntimeException(StringUtils.format("Invalid notification property configured. Only 'discovery' or URL is allowed, but got [%s]", service));
    }

    @Bean
    public Module alertingEvaluatorModule() {
        return new Module() {
            @Override
            public String getModuleName() {
                return "alerting-evaluator";
            }

            @Override
            public Version version() {
                return Version.unknownVersion();
            }

            @Override
            public void setupModule(SetupContext context) {
                context.registerSubtypes(AlertStateLocalMemoryStorage.class,
                                         AlertStateRedisStorage.class);
            }
        };
    }
}
