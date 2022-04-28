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

package org.bithon.server.alerting.common;

import feign.Client;
import feign.Contract;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.bithon.server.web.service.api.IDataSourceApi;
import org.bithon.server.web.service.meta.api.IMetadataApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author frankchen
 * @date 2020-03-21 00:22:54
 */
@Configuration
@EnableFeignClients
@Import(FeignClientsConfiguration.class)
public class RpcAutoConfiguration {
    /**
     * create this bean only if its implementation can't be found in current application
     * If the implementation can be found, it means this application is running with collector module
     */
    @Bean
    @ConditionalOnMissingBean(value = IDataSourceApi.class)
    public IDataSourceApi dataSourceApi(Contract contract,
                                        Client client,
                                        Encoder encoder,
                                        Decoder decoder) {
        return Feign.builder()
                    .client(client)
                    .contract(contract)
                    .encoder(encoder)
                    .decoder(decoder)
                    .target(IDataSourceApi.class, "http://localhost:9897");
    }

    @Bean
    @ConditionalOnMissingBean(value = IMetadataApi.class)
    public IMetadataApi metadataApi(Contract contract,
                                    Client client,
                                    Encoder encoder,
                                    Decoder decoder) {
        return Feign.builder()
                    .client(client)
                    .contract(contract)
                    .encoder(encoder)
                    .decoder(decoder)
                    .target(IMetadataApi.class, "http://localhost:9897");
    }
}
