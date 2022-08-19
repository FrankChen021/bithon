package org.bithon.server.webapp.services;

import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.time.DateTime;
import org.bithon.server.storage.web.Dashboard;
import org.bithon.server.storage.web.IDashboardStorage;
import org.bithon.server.webapp.WebAppModuleEnabler;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Frank Chen
 * @date 19/8/22 2:36 pm
 */
@Slf4j
@Service
@Conditional(value = WebAppModuleEnabler.class)
public class DashboardManager implements InitializingBean, DisposableBean {
    public interface IDashboardListener {
        void onChanged();
    }

    private final IDashboardStorage storage;
    private final ScheduledExecutorService loaderScheduler;
    private long lastLoadAt;
    private final Map<String, Dashboard> dashboards = new ConcurrentHashMap<>(9);

    private final List<IDashboardListener> listeners = Collections.synchronizedList(new ArrayList<>());

    public DashboardManager(IDashboardStorage storage) {
        this.storage = storage;

        loaderScheduler = Executors.newSingleThreadScheduledExecutor(NamedThreadFactory.of("dashboard-loader"));
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
    public void destroy() throws Exception {
        loaderScheduler.shutdownNow();
    }

    @Override
    public void afterPropertiesSet() {
        log.info("Starting schema incremental loader...");
        loaderScheduler.scheduleWithFixedDelay(this::incrementalLoad,
                                               0, // no delay to execute the first task
                                               1,
                                               TimeUnit.MINUTES);
    }

    private void incrementalLoad() {
        try {
            List<Dashboard> changedDashboards = storage.getDashboard(this.lastLoadAt);
            log.info("{} dashboards have been changed since {}.", changedDashboards.size(), DateTime.toYYYYMMDDhhmmss(this.lastLoadAt));

            if (!changedDashboards.isEmpty()) {
                for (Dashboard dashboard : changedDashboards) {
                    this.dashboards.put(dashboard.getName(), dashboard);
                }

                onChanged();
            }

            this.lastLoadAt = System.currentTimeMillis();
        } catch (Exception e) {
            log.error("Exception occurs when loading schemas", e);
        }
    }

    public Dashboard getDashboard(String boardName) {
        return dashboards.get(boardName);
    }

    public List<Dashboard> getDashboards() {
        return new ArrayList<>(dashboards.values());
    }

    public void addListener(IDashboardListener listener) {
        this.listeners.add(listener);
    }

    private void onChanged() {
        IDashboardListener[] listeners = this.listeners.toArray(new IDashboardListener[0]);
        for (IDashboardListener listener : listeners) {
            try {
                listener.onChanged();
            } catch (Exception e) {
                log.error("onChanged", e);
            }
        }
    }
}
