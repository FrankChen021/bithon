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

package org.bithon.server.web.service.datasource.api;

import org.bithon.server.storage.metrics.IMetricStorage;
import org.bithon.server.web.service.WebServiceModuleEnabler;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 8/1/22 9:57 PM
 */
@Service
@Conditional(WebServiceModuleEnabler.class)
public class DataSourceService {

    private final IMetricStorage metricStorage;

    public DataSourceService(IMetricStorage metricStorage) {
        this.metricStorage = metricStorage;
    }

    public List<String> getBaseline() {
        return metricStorage.getBaselineDates();
    }

    public void addToBaseline(String date, int keepDays) {
        metricStorage.saveBaseline(date, keepDays);
    }
}
