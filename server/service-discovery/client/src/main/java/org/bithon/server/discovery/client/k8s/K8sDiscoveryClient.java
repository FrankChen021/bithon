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

package org.bithon.server.discovery.client.k8s;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.OptBoolean;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1EndpointAddress;
import io.kubernetes.client.openapi.models.V1EndpointSubset;
import io.kubernetes.client.openapi.models.V1Endpoints;
import io.kubernetes.client.openapi.models.V1EndpointsList;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceList;
import io.kubernetes.client.util.Config;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.server.discovery.client.DiscoveredServiceInstance;
import org.bithon.server.discovery.client.IDiscoveryClient;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A Kubernetes-based implementation of the {@link IDiscoveryClient} interface.
 * This client discovers service instances by querying the Kubernetes API.
 *
 * @author frank.chen
 */
@Slf4j
public class K8sDiscoveryClient implements IDiscoveryClient {

    private final CoreV1Api api;
    private final String namespace;

    @JsonCreator
    public K8sDiscoveryClient(@JacksonInject(useInput = OptBoolean.FALSE) K8sDiscoveryProperties k8sDiscoveryProperties) {
        this.namespace = k8sDiscoveryProperties.getNamespace();

        try {
            ApiClient client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);
            this.api = new CoreV1Api();
        } catch (IOException e) {
            log.error("Failed to initialize Kubernetes API client", e);
            throw new RuntimeException("Failed to initialize Kubernetes API client", e);
        }
    }

    @Override
    public List<DiscoveredServiceInstance> getInstanceList() {
        return List.of();
    }

    @Override
    public List<DiscoveredServiceInstance> getInstanceList(String serviceName) {
        try {
            // First, find all services with the bithon service label
            V1ServiceList serviceList = api.listNamespacedService(
                namespace,
                null,
                null,
                null,
                null,
                "bithon.service." + serviceName + "=true",
                null,
                null,
                null,
                null,
                null
            );

            if (serviceList.getItems().isEmpty()) {
                throw new HttpMappableException(HttpStatus.SERVICE_UNAVAILABLE.value(),
                                                "Not found any instance of service [%s]", serviceName);
            }

            List<DiscoveredServiceInstance> instances = new ArrayList<>();

            // For each service, get the endpoints to find IPs and ports
            for (V1Service service : serviceList.getItems()) {
                String serviceLabelName = service.getMetadata().getName();
                Map<String, String> annotations = service.getMetadata().getAnnotations();
                String contextPath = annotations != null ? annotations.get("bithon.service.context-path") : null;

                V1EndpointsList endpointsList = api.listNamespacedEndpoints(
                    namespace,
                    null,
                    null,
                    null,
                    null,
                    "metadata.name=" + serviceLabelName,
                    null,
                    null,
                    null,
                    null,
                    null
                );

                for (V1Endpoints endpoints : endpointsList.getItems()) {
                    for (V1EndpointSubset subset : endpoints.getSubsets()) {
                        int port = subset.getPorts().get(0).getPort();

                        for (V1EndpointAddress address : subset.getAddresses()) {
                            instances.add(new DiscoveredServiceInstance(
                                serviceName,
                                address.getIp(),
                                port,
                                contextPath
                            ));
                        }
                    }
                }
            }

            if (instances.isEmpty()) {
                throw new HttpMappableException(HttpStatus.SERVICE_UNAVAILABLE.value(),
                                                "Found service [%s] but no active endpoints", serviceName);
            }

            return instances;
        } catch (ApiException e) {
            throw new HttpMappableException(e,
                                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                            "Failed to find instances of service [%s]: [%s]", serviceName, e.getMessage());
        }
    }
}
