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

package org.bithon.server.web.service.dashboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.concurrency.ScheduledExecutorServiceFactory;
import org.bithon.component.commons.time.DateTime;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.dashboard.Dashboard;
import org.bithon.server.storage.dashboard.IDashboardStorage;
import org.bithon.server.web.service.WebServiceModuleEnabler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Frank Chen
 * @date 19/8/22 2:36 pm
 */
@Slf4j
@Service
@Conditional(WebServiceModuleEnabler.class)
@ConditionalOnBean(IDashboardStorage.class)
public class DashboardManager implements SmartLifecycle {

    public interface IDashboardChangedListener {
        void onChanged();
    }

    private final ObjectMapper objectMapper;
    private final IDashboardStorage storage;
    private ScheduledExecutorService loaderScheduler;
    private long lastLoadAt;
    private final Map<String, Dashboard> dashboards = new ConcurrentHashMap<>(9);

    private final List<IDashboardChangedListener> listeners = Collections.synchronizedList(new ArrayList<>());

    public DashboardManager(ObjectMapper objectMapper, IDashboardStorage storage) {
        this.objectMapper = objectMapper;
        this.storage = storage;
    }

    public void update(String name, String payload) {
        String sig = this.storage.put(name, payload);

        this.dashboards.put(name, Dashboard.builder()
                                           .name(name)
                                           .payload(payload)
                                           .signature(sig)
                                           .build());
        this.onChanged();
    }

    @Override
    public void start() {
        log.info("Starting dashboard incremental loader...");

        loaderScheduler = ScheduledExecutorServiceFactory.newSingleThreadScheduledExecutor(NamedThreadFactory.nonDaemonThreadFactory("dashboard-loader"));
        loaderScheduler.scheduleWithFixedDelay(this::incrementalLoad,
                                               // no delay to execute the first task
                                               0,
                                               30,
                                               TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        if (loaderScheduler != null) {
            loaderScheduler.shutdownNow();
            try {
                //noinspection ResultOfMethodCallIgnored
                loaderScheduler.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            } finally {
                loaderScheduler = null;
            }
        }
    }

    @Override
    public boolean isRunning() {
        return loaderScheduler != null && !loaderScheduler.isShutdown();
    }

    private void incrementalLoad() {
        long now = TimeSpan.now().toSeconds() * 1000;

        List<Dashboard> changedDashboards = storage.getDashboard(this.lastLoadAt);
        log.info("{} dashboards have been changed since {}.", changedDashboards.size(), DateTime.toYYYYMMDDhhmmss(this.lastLoadAt));

        if (!changedDashboards.isEmpty()) {
            for (Dashboard dashboard : changedDashboards) {
                if (dashboard.isDeleted()) {
                    this.dashboards.remove(dashboard.getName());
                } else {
                    dashboard.setMetadata(getMetadata(dashboard));

                    this.dashboards.put(dashboard.getName(), dashboard);
                }
            }

            onChanged();
        }

        this.lastLoadAt = now;
    }

    private Dashboard.Metadata getMetadata(Dashboard dashboard) {
        try {
            return objectMapper.readValue(dashboard.getPayload(), Dashboard.Metadata.class);
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    public Dashboard getDashboard(String boardName) {
        return dashboards.get(boardName);
    }

    public List<Dashboard> getDashboards() {
        return new ArrayList<>(dashboards.values());
    }

    public void addChangedListener(IDashboardChangedListener listener) {
        this.listeners.add(listener);
    }

    private void onChanged() {
        IDashboardChangedListener[] listeners = this.listeners.toArray(new IDashboardChangedListener[0]);
        for (IDashboardChangedListener listener : listeners) {
            try {
                listener.onChanged();
            } catch (Exception e) {
                log.error("onChanged", e);
            }
        }
    }
}
