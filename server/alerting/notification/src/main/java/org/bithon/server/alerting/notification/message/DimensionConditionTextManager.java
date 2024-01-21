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

package org.bithon.server.alerting.notification.message;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bithon.server.storage.datasource.filter.IColumnFilter;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * @author frank.chen021@outlook.com
 */
@Service
public class DimensionConditionTextManager {

    private final LoadingCache<Key, String> textCache;

    public DimensionConditionTextManager(IDataSourceApi dataSourceApi) {
        textCache = Caffeine.newBuilder()
                            .expireAfterWrite(Duration.ofHours(1))
                            .build(key -> key.getFilter()
                                             .accept(new DimensionConditionTextBuilder(key.getFilter().getField(),
                                                                                       dataSourceApi.getSchemaByName(key.getDataSource()))));
    }

    public String getDisplayText(String dataSource, IColumnFilter filter) {
        return textCache.get(new Key(dataSource, filter));
    }

    @Data
    @EqualsAndHashCode
    @AllArgsConstructor
    private static class Key {
        private String dataSource;
        private IColumnFilter filter;
    }

}
