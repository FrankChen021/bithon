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
import org.bithon.server.alerting.evaluator.evaluator.AlertEvaluator;
import org.bithon.server.alerting.evaluator.evaluator.EvaluationLogBatchWriter;
import org.bithon.server.alerting.evaluator.evaluator.INotificationApiInvoker;
import org.bithon.server.alerting.evaluator.repository.AlertRepository;
import org.bithon.server.alerting.evaluator.state.local.LocalStateManager;
import org.bithon.server.storage.alerting.AlertingStorageConfiguration;
import org.bithon.server.storage.alerting.IAlertRecordStorage;
import org.bithon.server.storage.alerting.IEvaluationLogStorage;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * @author Frank Chen
 * @date 12/11/21 6:18 pm
 */
@Configuration
@Conditional(EvaluatorModuleEnabler.class)
@ImportAutoConfiguration(value = {AlertingStorageConfiguration.class})
public class EvaluatorModuleAutoConfiguration {

    @Bean
    public AlertEvaluator alertEvaluator(AlertRepository repository,
                                         IEvaluationLogStorage logStorage,
                                         IAlertRecordStorage recordStorage,
                                         IDataSourceApi dataSourceApi,
                                         ServerProperties serverProperties,
                                         INotificationApiInvoker notificationApiInvoker,
                                         ObjectMapper objectMapper) {

        EvaluationLogBatchWriter logWriter = new EvaluationLogBatchWriter(logStorage.createWriter(), Duration.ofSeconds(5), 10000);
        logWriter.start();

        return new AlertEvaluator(repository,
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
