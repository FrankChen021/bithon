/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.server.meta.storage.jdbc;

import com.sbss.bithon.component.db.dao.MetadataDAO;
import com.sbss.bithon.component.db.jooq.tables.records.BithonMetadataRecord;
import com.sbss.bithon.server.common.pojo.DisplayableText;
import com.sbss.bithon.server.common.utils.datetime.DateTimeUtils;
import com.sbss.bithon.server.meta.EndPointLink;
import com.sbss.bithon.server.meta.Metadata;
import com.sbss.bithon.server.meta.MetadataType;
import com.sbss.bithon.server.meta.storage.IMetaStorage;
import org.jooq.DSLContext;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/11 10:56 下午
 */
public class MetadataJdbcStorage implements IMetaStorage {

    private final MetadataDAO metadataDao;

    public MetadataJdbcStorage(DSLContext dsl) {
        this.metadataDao = new MetadataDAO(dsl);
    }

    @Override
    public long getOrCreateMetadataId(String name, MetadataType type, long parent) {
        return metadataDao.upsertMetadata(name, type.getType(), parent);
    }

    @Override
    public Collection<Metadata> getMetadataByType(MetadataType type) {
        return metadataDao.getMetadata(type.getType())
                          .stream()
                          .map(r -> new Metadata(r.getName(), r.getType(), r.getParentId()))
                          .collect(Collectors.toList());
    }

    @Override
    public long createMetricDimension(String dataSource,
                                      String dimensionName,
                                      String dimensionValue,
                                      long timestamp) {
        return metadataDao.upsertDimension(dataSource, dimensionName, dimensionValue, timestamp);
    }

    @Override
    public long createTopo(EndPointLink link) {
        return metadataDao.upsertTopo(link.getSrcEndpointType(),
                                      link.getSrcEndpoint(),
                                      link.getDstEndpointType(),
                                      link.getDstEndpoint());
    }

    @Override
    public String getApplicationByInstance(String instanceName) {
        BithonMetadataRecord application = metadataDao.getMetaByNameAndType(instanceName,
                                                                            MetadataType.APP_INSTANCE.getType());
        return application == null ? null : application.getName();
    }

    @Override
    public boolean isApplicationExist(String applicationName) {
        BithonMetadataRecord application = metadataDao.getMetaByNameAndType(applicationName,
                                                                            MetadataType.APPLICATION.getType());
        return application != null;
    }

    @Override
    public Collection<DisplayableText> getMetricDimensions(String dataSourceName,
                                                           String dimensionName,
                                                           String startISO8601,
                                                           String endISO8601) {
        try {
            return metadataDao.getMetricDimensions(dataSourceName,
                                                   dimensionName,
                                                   DateTimeUtils.fromISO8601(startISO8601),
                                                   DateTimeUtils.fromISO8601(endISO8601))
                              .stream()
                              .map(r -> new DisplayableText(r.getId().toString(),
                                                            r.getDimensionValue()))
                              .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
