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

import feign.Contract;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.discovery.client.DiscoveredServiceInvoker;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/1/5 17:25
 */
public interface INotificationApiStub extends INotificationApi {

    /**
     * Always inject the notification api invoker 'because it does not strongly rely on the notification service but just a RPC stub
     */
    @Configuration
    @EnableFeignClients
    @Import(FeignClientsConfiguration.class)
    class AutoConfiguration {

        @Bean
        public static INotificationApiStub notificationApiStub(ApplicationContext context) {
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

            // Wrap the implementation with a notification invoker
            return impl::notify;

            /*
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
            };*/
        }
    }
}
