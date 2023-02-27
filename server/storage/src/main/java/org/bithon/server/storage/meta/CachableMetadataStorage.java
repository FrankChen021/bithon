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

package org.bithon.server.storage.meta;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.time.Duration;
import java.util.Collection;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/4 12:15 上午
 */
public class CachableMetadataStorage implements IMetaStorage {

    private final IMetaStorage delegate;
    private final LoadingCache<String, Boolean> applicationCache;
    // TODO: replace with LoadingCache
    private final Cache<Metadata, Long> metaCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(1)).build();
    private final Cache<String, String> instanceCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(5)).build();

    // avoid frequent queries on underlying storage if instance is not found
    private final Cache<String, Boolean> instanceNotExistCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(1)).build();

    public CachableMetadataStorage(IMetaStorage delegate) {
        this.delegate = delegate;

        applicationCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(1)).build(this.delegate::isApplicationExist);
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
    public String getApplicationByInstance(String instanceName) {
        Boolean notFound = instanceNotExistCache.getIfPresent(instanceName);
        if (notFound != null) {
            return null;
        }

        String applicationName = instanceCache.getIfPresent(instanceName);
        if (applicationName != null) {
            return applicationName;
        }

        // Use lock to avoid frequent queries on underlying storage
        synchronized (this) {
            // double check first
            applicationName = instanceCache.getIfPresent(instanceName);
            if (applicationName == null) {
                applicationName = delegate.getApplicationByInstance(instanceName);
                if (applicationName != null) {
                    instanceCache.put(instanceName, applicationName);
                    instanceNotExistCache.invalidate(instanceName);
                } else {
                    instanceNotExistCache.put(instanceName, true);
                }
            }
        }

        return applicationName;
    }

    @Override
    public boolean isApplicationExist(String applicationName) {
        Boolean exists = applicationCache.get(applicationName);
        return exists != null && exists;
    }

    @Override
    public void initialize() {
        delegate.initialize();
    }

    @Override
    public Collection<Metadata> getApplications(String appType, long since) {
        return delegate.getApplications(appType, since);
    }
}
