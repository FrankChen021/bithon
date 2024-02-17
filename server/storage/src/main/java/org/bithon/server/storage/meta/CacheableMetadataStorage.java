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

import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.concurrency.PeriodicTask;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.common.expiration.IExpirationRunnable;
import org.springframework.beans.factory.DisposableBean;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/4 12:15 上午
 */
@Slf4j
public class CacheableMetadataStorage implements IMetaStorage, DisposableBean {

    private final IMetaStorage delegate;

    private PeriodicTask loadInstanceTask;
    private SaveInstanceTask saveInstanceTask;

    private Set<String> applicationCache = Collections.emptySet();
    private Map<String, String> instance2AppCache = new ConcurrentHashMap<>();
    private Set<Instance> instanceCache = new ConcurrentSkipListSet<>();

    public CacheableMetadataStorage(IMetaStorage delegate) {
        this.delegate = delegate;
    }

    @Override
    public void saveApplicationInstance(Collection<Instance> instanceList) {
        // Filter out those not in the cache
        instanceList = instanceList.stream()
                                   .filter(instance -> !instanceCache.contains(instance))
                                   .collect(Collectors.toList());

        if (instanceList.isEmpty()) {
            return;
        }

        this.saveInstanceTask.add(instanceList);

        //
        // Update cache in current running instance
        //
        instanceCache.addAll(instanceList);
        instanceList.forEach((instance -> instance2AppCache.put(instance.getInstanceName(), instance.getAppName())));
    }

    @Override
    public String getApplicationByInstance(String instanceName) {
        return this.instance2AppCache.get(instanceName);
    }

    @Override
    public boolean isApplicationExist(String applicationName) {
        return applicationCache.contains(applicationName);
    }

    @Override
    public Collection<Instance> getApplicationInstances(long since) {
        // Application should not call this
        return Collections.emptyList();
    }

    @Override
    public Collection<Metadata> getApplications(String appType, long since) {
        return delegate.getApplications(appType, since);
    }

    @Override
    public void initialize() {
        delegate.initialize();

        this.loadInstanceTask = new LoadInstanceTask(Duration.ofMinutes(1));
        this.loadInstanceTask.start();

        this.saveInstanceTask = new SaveInstanceTask(100, Duration.ofSeconds(10));
        this.saveInstanceTask.start();
    }

    @Override
    public void destroy() throws Exception {
        this.loadInstanceTask.stop();
        this.saveInstanceTask.stop();
    }

    @Override
    public IExpirationRunnable getExpirationRunnable() {
        return delegate.getExpirationRunnable();
    }

    private class SaveInstanceTask extends PeriodicTask {
        private Set<Instance> instanceList;
        private Set<Instance> savedInstanceList;
        private final int batchSize;

        public SaveInstanceTask(int batchSize, Duration period) {
            super("bi-meta-saver", period, false);
            this.batchSize = batchSize;
            this.instanceList = Collections.synchronizedSet(new HashSet<>());
        }

        public void add(Collection<Instance> instanceList) {
            this.instanceList.addAll(instanceList);
            if (this.instanceList.size() >= batchSize) {
                this.runImmediately();
            }
        }

        @Override
        protected void onRun() {
            if (instanceList.isEmpty()) {
                return;
            }

            this.savedInstanceList = this.instanceList;
            this.instanceList = Collections.synchronizedSet(new HashSet<>());

            delegate.saveApplicationInstance(savedInstanceList);
        }

        @Override
        protected void onException(Exception e) {
            log.error("Failed to save instances", e);

            // add the saved back to instanceList for next round to save
            this.instanceList.addAll(this.savedInstanceList);
        }

        @Override
        protected void onStopped() {
            log.info("Task [{}] stopped.", getName());

            // Make sure data in the list has been flushed
            this.onRun();
        }
    }

    private class LoadInstanceTask extends PeriodicTask {
        public LoadInstanceTask(Duration period) {
            super("bi-meta-loader", period, false);
        }

        @Override
        protected void onRun() {
            log.info("Loading all application instances...");

            // The metadata API retrieves data by the latest 24H, it SHOULD be optimized by reading the corresponding data source
            // Since it has not been changed yet, here we simply keep the instance updated in 24H
            Collection<Instance> instances = delegate.getApplicationInstances(TimeSpan.now().before(1, TimeUnit.DAYS).getMilliseconds());

            // do not use stream API to collect map because the instances may contain duplicated items of the same instance ip
            Set<String> applicationCache = new HashSet<>();
            Map<String, String> instance2App = new ConcurrentHashMap<>();
            for (Instance instance : instances) {
                instance2App.put(instance.getInstanceName(), instance.getAppName());

                applicationCache.add(instance.getAppName());
            }

            CacheableMetadataStorage.this.instance2AppCache = instance2App;
            CacheableMetadataStorage.this.instanceCache = new ConcurrentSkipListSet<>(instances);
            CacheableMetadataStorage.this.applicationCache = applicationCache;
        }

        @Override
        protected void onException(Exception e) {
            log.error("Failed to load instances", e);
        }

        @Override
        protected void onStopped() {
            log.info("Task [{}] stopped.", getName());
        }
    }
}
