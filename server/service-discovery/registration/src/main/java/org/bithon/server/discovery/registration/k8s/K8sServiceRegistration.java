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

import com.alibaba.cloud.commons.lang.StringUtils;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.Config;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.commons.spring.EnvironmentBinder;
import org.bithon.server.discovery.declaration.DiscoverableService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Kubernetes service registration for Bithon services.
 *
 * @author Frank Chen
 */
@Slf4j
@Configuration
public class K8sServiceRegistration {

    @Bean
    @ConditionalOnProperty(value = "bithon.discovery.type", havingValue = "k8s")
    public K8sServiceRegistrationRunner k8sServiceRegistrationRunner(ApplicationContext applicationContext,
                                                                     ServerProperties serverProperties,
                                                                     EnvironmentBinder binder) {
        K8sServiceRegistrationProperties properties = binder.bind("bithon.discovery.kubernetes", K8sServiceRegistrationProperties.class);
        String contextPath = binder.bind("server.servlet.context-path", String.class);

        return new K8sServiceRegistrationRunner(applicationContext, serverProperties.getPort(), contextPath, properties);
    }

    public static class K8sServiceRegistrationRunner {

        private final ApplicationContext applicationContext;
        private final CoreV1Api api;
        private final int port;
        private final String contextPath;
        private final K8sServiceRegistrationProperties properties;

        public K8sServiceRegistrationRunner(ApplicationContext applicationContext,
                                            int port,
                                            String contextPath,
                                            K8sServiceRegistrationProperties properties) {
            this.applicationContext = applicationContext;
            this.contextPath = contextPath;
            this.port = port;
            this.properties = properties;

            try {
                ApiClient client = Config.defaultClient();
                io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);
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
                // Create labels map based on discovered services
                Map<String, String> serviceLabels = new HashMap<>();

                Map<String, Object> serviceProviders = applicationContext.getBeansWithAnnotation(DiscoverableService.class);
                for (Map.Entry<String, Object> entry : serviceProviders.entrySet()) {
                    Object serviceProvider = entry.getValue();
                    Class<?> serviceClazz = serviceProvider.getClass();

                    // Root parent must be Object in Java
                    while (serviceClazz != null) {
                        for (Class<?> interfaceClazz : serviceClazz.getInterfaces()) {
                            DiscoverableService annotation = interfaceClazz.getAnnotation(DiscoverableService.class);
                            if (annotation != null) {
                                serviceLabels.put("bithon.service." + annotation.name(), "true");
                            }
                        }
                        serviceClazz = serviceClazz.getSuperclass();
                    }
                }

                if (serviceLabels.isEmpty()) {
                    log.info("No discoverable services found to register");
                    return;
                }

                if (StringUtils.isNotEmpty(contextPath)) {
                    serviceLabels.put("bithon.service.context-path", contextPath);
                }
                serviceLabels.put("bithon.service.port", String.valueOf(this.port));

                // Only update pod, skip service update
                updatePod(serviceLabels);

            } catch (Exception e) {
                log.error("Failed to register Bithon services", e);
            }
        }

        private void updatePod(Map<String, String> serviceLabels) {
            log.info("Updating pod with service labels: {}", serviceLabels);

            try {
                // Get current pod name from environment variable
                String podName = System.getenv("HOSTNAME");
                if (podName == null) {
                    log.warn("Cannot determine current pod name from HOSTNAME environment variable");
                    return;
                }

                String namespace = properties.getNamespace();

                // Get the current pod - using the API compatible with version 19.0.1
                V1Pod pod = api.readNamespacedPod(podName, namespace, null);
                if (pod == null) {
                    log.warn("Pod [{}] not found in namespace [{}], cannot update pod labels", podName, namespace);
                    return;
                }

                // Get existing labels or create a new map
                V1ObjectMeta metadata = pod.getMetadata();
                Map<String, String> labels = metadata.getLabels();
                if (labels == null) {
                    labels = new HashMap<>();
                    metadata.setLabels(labels);
                }
                labels.putAll(serviceLabels);

                // Update the pod in Kubernetes - using the API compatible with version 19.0.1
                api.replaceNamespacedPod(podName, namespace, pod, null, null, null, null);
                log.info("Updated pod [{}] in namespace [{}] with Bithon service labels", podName, namespace);
            } catch (ApiException e) {
                log.error("Failed to update pod with service labels:" + e.getMessage() + "\n" + e.getResponseBody(), e);
            }
        }
    }
}
