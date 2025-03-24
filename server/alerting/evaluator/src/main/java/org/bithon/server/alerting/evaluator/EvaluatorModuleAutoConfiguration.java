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
import feign.Contract;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.evaluator.EvaluationLogger;
import org.bithon.server.alerting.evaluator.evaluator.AlertEvaluator;
import org.bithon.server.alerting.evaluator.evaluator.EvaluationLogBatchWriter;
import org.bithon.server.alerting.evaluator.evaluator.INotificationApiInvoker;
import org.bithon.server.alerting.evaluator.repository.AlertRepository;
import org.bithon.server.alerting.evaluator.state.IEvaluationStateManager;
import org.bithon.server.alerting.evaluator.state.local.LocalStateManager;
import org.bithon.server.alerting.notification.api.INotificationApi;
import org.bithon.server.alerting.notification.message.NotificationMessage;
import org.bithon.server.discovery.client.DiscoveredServiceInvoker;
import org.bithon.server.storage.alerting.AlertingStorageConfiguration;
import org.bithon.server.storage.alerting.IAlertRecordStorage;
import org.bithon.server.storage.alerting.IAlertStateStorage;
import org.bithon.server.storage.alerting.IEvaluationLogStorage;
import org.bithon.server.storage.alerting.IEvaluationLogWriter;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import java.time.Duration;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
    public IEvaluationStateManager stateManager(IAlertStateStorage stateStorage) {
        return new LocalStateManager(stateStorage);
    }

    @Bean
    public INotificationApiInvoker createNotificationAsyncInvoker(ApplicationContext context,
                                                                  IEvaluationLogStorage logStorage) {
        INotificationApi impl;

        // The notification service is configured by auto-discovery
        String service = context.getBean(Environment.class).getProperty("bithon.alerting.evaluator.notification-service", "discovery");
        if ("discovery".equalsIgnoreCase(service)) {
            // Even the notification module is deployed with the evaluator module together,
            // we still call the notification module via HTTP instead of direct API method calls in process
            // So that it simulates the 'remote call' via discovered service
            DiscoveredServiceInvoker invoker = context.getBean(DiscoveredServiceInvoker.class);
            impl = invoker.createUnicastApi(INotificationApi.class);
        } else if (service.startsWith("http:") || service.startsWith("https:")) {
            // The service is configured as a remote service at fixed address
            // Create a feign client to call it
            impl = Feign.builder()
                        .contract(context.getBean(Contract.class))
                        .encoder(context.getBean(Encoder.class))
                        .decoder(context.getBean(Decoder.class))
                        .target(INotificationApi.class, service);
        } else {
            throw new RuntimeException(StringUtils.format("Invalid notification property configured. Only 'discovery' or URL is allowed, but got [%s]", service));
        }

        return new INotificationApiInvoker() {
            // Cached thread pool
            private final ThreadPoolExecutor notificationThreadPool = new ThreadPoolExecutor(1,
                                                                                             10,
                                                                                             3,
                                                                                             TimeUnit.MINUTES,
                                                                                             new SynchronousQueue<>(),
                                                                                             NamedThreadFactory.nonDaemonThreadFactory("notification"),
                                                                                             new ThreadPoolExecutor.CallerRunsPolicy());


            @Override
            public void notify(String name, NotificationMessage message) {
                notificationThreadPool.execute(() -> {
                    try {
                        impl.notify(name, message);
                    } catch (Exception e) {
                        try (IEvaluationLogWriter writer = logStorage.createWriter()) {
                            new EvaluationLogger(writer).error(message.getAlertRule().getId(),
                                                               message.getAlertRule().getName(),
                                                               AlertEvaluator.class,
                                                               e,
                                                               "Failed to send notification to channel [%s]",
                                                               name);
                        }
                    }
                });
            }
        };
    }

    @Bean
    public AlertEvaluator alertEvaluator(AlertRepository repository,
                                         IEvaluationStateManager stateManager,
                                         IEvaluationLogStorage logStorage,
                                         IAlertRecordStorage recordStorage,
                                         IDataSourceApi dataSourceApi,
                                         ServerProperties serverProperties,
                                         INotificationApiInvoker notificationApiInvoker,
                                         ObjectMapper objectMapper) {

        EvaluationLogBatchWriter logWriter = new EvaluationLogBatchWriter(logStorage.createWriter(), Duration.ofSeconds(5), 10000);
        logWriter.start();

        return new AlertEvaluator(repository,
                                  stateManager,
                                  logWriter,
                                  recordStorage,
                                  dataSourceApi,
                                  serverProperties,
                                  notificationApiInvoker,
                                  objectMapper);
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
                context.registerSubtypes(LocalStateManager.class
                );
            }
        };
    }
}
