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

package org.bithon.server.sink.meta;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.bithon.server.storage.meta.IMetaStorage;
import org.bithon.server.storage.meta.Metadata;
import org.bithon.server.storage.meta.MetadataType;

import java.time.Duration;
import java.util.Collection;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/4 12:15 上午
 */
public class CachableMetadataStorage implements IMetaStorage {

    private final IMetaStorage delegate;
    // TODO: replace with LoadingCache
    private final Cache<Metadata, Long> metaCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(1)).build();
    private final Cache<String, String> instanceCache = Caffeine.newBuilder()
                                                                .expireAfterWrite(Duration.ofMinutes(5))
                                                                .build();

    public CachableMetadataStorage(IMetaStorage delegate) {
        this.delegate = delegate;
    }

    // TODO:
    //
    // 控制写入TPS
    // 中间采用一个队列，新数据覆盖队列中的数据（时间）
    @Override
    public void saveApplicationInstance(String applicationName, String applicationType, String instance) {
        Metadata key = new Metadata(applicationName, applicationType);
        Long id = metaCache.getIfPresent(key);
        if (id == null) {
            synchronized (this) {
                if (metaCache.getIfPresent(key) != null) {
                    return;
                }
                delegate.saveApplicationInstance(applicationName, applicationType, instance);
                metaCache.put(key, System.currentTimeMillis());
            }
        }
    }

    @Override
    public Collection<Metadata> getMetadataByType(MetadataType type) {
        return delegate.getMetadataByType(type);
    }

    @Override
    public String getApplicationByInstance(String instanceName) {
        String applicationName = instanceCache.getIfPresent(instanceName);
        if (applicationName == null) {
            applicationName = delegate.getApplicationByInstance(instanceName);
            if (applicationName != null) {
                instanceCache.put(instanceName, applicationName);
            }
            return applicationName;
        }
        return applicationName;
    }

    @Override
    public boolean isApplicationExist(String applicationName) {
        //TODO: cache
        return delegate.isApplicationExist(applicationName);
    }

    @Override
    public void initialize() {
        delegate.initialize();
    }
}
