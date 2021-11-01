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

package org.bithon.server.storage.jdbc.meta;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.bithon.component.db.dao.MetadataDAO;
import org.bithon.component.db.jooq.tables.records.BithonApplicationInstanceRecord;
import org.bithon.server.meta.Metadata;
import org.bithon.server.meta.MetadataType;
import org.bithon.server.meta.storage.IMetaStorage;
import org.jooq.DSLContext;

import java.util.Collection;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/11 10:56 下午
 */
@JsonTypeName("jdbc")
public class MetadataJdbcStorage implements IMetaStorage {

    protected final MetadataDAO metadataDao;

    @JsonCreator
    public MetadataJdbcStorage(@JacksonInject(useInput = OptBoolean.FALSE) DSLContext dsl) {
        this.metadataDao = new MetadataDAO(dsl);
    }

    @Override
    public void saveApplicationInstance(String applicationName, String applicationType, String instance) {
        metadataDao.saveApplicationInstance(applicationName, applicationType, instance);
    }

    @Override
    public Collection<Metadata> getMetadataByType(MetadataType type) {
        long day = 3600_000 * 24;
        return metadataDao.getApplications(System.currentTimeMillis() - day, Metadata.class);
    }

    @Override
    public String getApplicationByInstance(String instanceName) {
        BithonApplicationInstanceRecord instance = metadataDao.getByInstanceName(instanceName);
        return instance == null ? null : instance.getAppname();
    }

    @Override
    public boolean isApplicationExist(String applicationName) {
        BithonApplicationInstanceRecord instance = metadataDao.getByApplicationName(applicationName);
        return instance != null;
    }
}
