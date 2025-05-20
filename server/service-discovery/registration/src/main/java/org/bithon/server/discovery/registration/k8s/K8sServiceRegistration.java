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

package org.bithon.server.discovery.registration.k8s;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.util.Config;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.commons.spring.EnvironmentBinder;
import org.bithon.server.discovery.declaration.DiscoverableService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Kubernetes service registration for Bithon services.
 *
 * @author Frank Chen
 */
@Slf4j
public class K8sServiceRegistration {

    @ConditionalOnProperty(prefix = "bithon.discovery.type", name = "k8s")
    public K8sServiceRegistrationRunner k8sServiceRegistrationRunner(ApplicationContext applicationContext,
                                                                     EnvironmentBinder binder) {
        K8sServiceRegistrationProperties properties = binder.bind("bithon.discovery.kubernetes", K8sServiceRegistrationProperties.class);
        String contextPath = binder.bind("server.servlet.context-path", String.class);

        return new K8sServiceRegistrationRunner(applicationContext, contextPath, properties);
    }

    public static class K8sServiceRegistrationRunner {

        private final ApplicationContext applicationContext;
        private final CoreV1Api api;
        private final String contextPath;
        private final K8sServiceRegistrationProperties properties;

        public K8sServiceRegistrationRunner(ApplicationContext applicationContext,
                                            String contextPath,
                                            K8sServiceRegistrationProperties properties) {
            this.applicationContext = applicationContext;
            this.contextPath = contextPath;
            this.properties = properties;

            try {
                ApiClient client = Config.defaultClient();
                Configuration.setDefaultApiClient(client);
                this.api = new CoreV1Api();

                // Register services as K8s service labels
                registerServices();
            } catch (IOException e) {
                log.error("Failed to initialize Kubernetes API client", e);
                throw new RuntimeException("Failed to initialize Kubernetes API client", e);
            }
        }

        private void registerServices() {
            try {
                // Get the current service
                V1Service service = api.readNamespacedService(
                    properties.getServiceName(),
                    properties.getNamespace(),
                    null
                );

                if (service == null) {
                    log.warn("Service [{}] not found in namespace [{}], cannot register Bithon services",
                             properties.getServiceName(), properties.getNamespace());
                    return;
                }

                // Get existing labels or create a new map
                V1ObjectMeta metadata = service.getMetadata();
                Map<String, String> labels = metadata.getLabels();
                if (labels == null) {
                    labels = new HashMap<>();
                    metadata.setLabels(labels);
                }

                // Get existing annotations or create a new map
                Map<String, String> annotations = metadata.getAnnotations();
                if (annotations == null) {
                    annotations = new HashMap<>();
                    metadata.setAnnotations(annotations);
                }

                // Store context path in annotations
                annotations.put("bithon.service.context-path", contextPath);

                // Find declared services and add as labels
                Map<String, Object> serviceProviders = applicationContext.getBeansWithAnnotation(DiscoverableService.class);
                boolean updated = false;

                for (Map.Entry<String, Object> entry : serviceProviders.entrySet()) {
                    Object serviceProvider = entry.getValue();
                    Class<?> serviceClazz = serviceProvider.getClass();

                    // Root parent must be Object in Java
                    while (serviceClazz != null) {
                        for (Class<?> interfaceClazz : serviceClazz.getInterfaces()) {
                            DiscoverableService annotation = interfaceClazz.getAnnotation(DiscoverableService.class);
                            if (annotation != null) {
                                labels.put("bithon.service." + annotation.name(), "true");
                                updated = true;
                            }
                        }
                        serviceClazz = serviceClazz.getSuperclass();
                    }
                }

                // If we found and added any services, update the service in Kubernetes
                if (updated) {
                    api.replaceNamespacedService(
                        properties.getServiceName(),
                        properties.getNamespace(),
                        service,
                        null,
                        null,
                        null,
                        null
                    );
                    log.info("Updated service [{}] in namespace [{}] with Bithon service labels",
                             properties.getServiceName(), properties.getNamespace());
                }
            } catch (ApiException e) {
                log.error("Failed to update service with Bithon service labels", e);
            }
        }
    }
}
