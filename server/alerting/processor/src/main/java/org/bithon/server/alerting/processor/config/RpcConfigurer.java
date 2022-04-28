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

package org.bithon.server.alerting.processor.config;

import feign.Client;
import feign.Contract;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.AlertingModule;
import org.bithon.server.web.service.api.IDataSourceApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/**
 * @author frankchen
 * @date 2020-03-21 00:22:54
 */
@Configuration
@EnableFeignClients
@Import(FeignClientsConfiguration.class)
@ConditionalOnBean(AlertingModule.class)
public class RpcConfigurer {
    @Bean
    @ConditionalOnMissingBean(value = IDataSourceApi.class)
    public IDataSourceApi dataSourceApi(Contract contract,
                                        Client client,
                                        Encoder encoder,
                                        Decoder decoder,
                                        Environment environment) {
        return Feign.builder()
                    .client(client)
                    .contract(contract)
                    .encoder(encoder)
                    .decoder(decoder)
                    .target(IDataSourceApi.class, StringUtils.format("http://%s", environment.getProperty("bithon.server.data-source", "localhost:9897")));
    }
}
