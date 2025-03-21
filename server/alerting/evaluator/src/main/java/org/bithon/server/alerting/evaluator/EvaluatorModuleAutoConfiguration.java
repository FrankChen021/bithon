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
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.alerting.common.evaluator.metric.relative.baseline.BaselineMetricCacheManager;
import org.bithon.server.alerting.evaluator.evaluator.AlertEvaluator;
import org.bithon.server.alerting.evaluator.evaluator.EvaluationLogBatchWriter;
import org.bithon.server.alerting.evaluator.repository.AlertRepository;
import org.bithon.server.alerting.evaluator.storage.local.AlertStateLocalMemoryStorage;
import org.bithon.server.alerting.evaluator.storage.redis.AlertStateRedisStorage;
import org.bithon.server.storage.alerting.AlertingStorageConfiguration;
import org.bithon.server.storage.alerting.IAlertRecordStorage;
import org.bithon.server.storage.alerting.IAlertStateStorage;
import org.bithon.server.storage.alerting.IEvaluationLogStorage;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.time.Duration;
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
    public BaselineMetricCacheManager cacheManager(IDataSourceApi dataSourceApi) {
        return new BaselineMetricCacheManager(dataSourceApi);
    }

    @Bean
    public AlertEvaluator alertEvaluator(AlertRepository repository,
                                         IAlertStateStorage stateStorage,
                                         IEvaluationLogStorage logStorage,
                                         IAlertRecordStorage recordStorage,
                                         IDataSourceApi dataSourceApi,
                                         ServerProperties serverProperties,
                                         ApplicationContext applicationContext,
                                         ObjectMapper objectMapper) {

        EvaluationLogBatchWriter logWriter = new EvaluationLogBatchWriter(logStorage.createWriter(), Duration.ofSeconds(5), 10000);
        logWriter.start();

        return new AlertEvaluator(repository,
                                  stateStorage,
                                  logWriter,
                                  recordStorage,
                                  dataSourceApi,
                                  serverProperties,
                                  applicationContext,
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
                context.registerSubtypes(AlertStateLocalMemoryStorage.class,
                                         AlertStateRedisStorage.class);
            }
        };
    }
}
