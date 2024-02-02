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

package org.bithon.agent.sentinel;

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.configuration.IConfigurationChangedListener;
import org.bithon.agent.sentinel.degrade.DegradingRuleDto;
import org.bithon.agent.sentinel.expt.SentinelCommandException;
import org.bithon.agent.sentinel.flow.FlowRuleDto;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.shaded.com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import org.bithon.shaded.com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

/**
 * @author frankchen
 */
public class SentinelRuleManager {

    private static final ILogAdaptor log = LoggerFactory.getLogger(SentinelRuleManager.class);
    private static volatile SentinelRuleManager INSTANCE;

    static {
        if (System.getProperty("csp.sentinel.log.output.type") == null) {
            System.setProperty("csp.sentinel.log.output.type", "console");
        }
        if (System.getProperty("csp.sentinel.metric.flush.interval") == null) {
            System.setProperty("csp.sentinel.metric.flush.interval", "0");
        }
    }

    final Map<String, CompositeRule> sentinelRules = new ConcurrentHashMap<>();
    /**
     * inverted index, used to find FlowRule by id
     */
    final Map<String, FlowRuleDto> flowId2Rules = new ConcurrentHashMap<>();
    final Map<String, DegradingRuleDto> degradeId2Rules = new ConcurrentHashMap<>();
    ISentinelListener listener;

    /**
     * Only used for unit tests
     */
    public SentinelRuleManager(ISentinelListener listener) {
        this.listener = listener;
    }

    private SentinelRuleManager() {
        ConfigurationManager manager = ConfigurationManager.getInstance();
        manager.addConfigurationChangeListener(new FlowRuleChangedListener());
        manager.addConfigurationChangeListener(new DegradingRuleChangedListener());

        refreshFlowRules();
        refreshDegradingRule();
    }

    public static SentinelRuleManager getInstance() {
        if (INSTANCE == null) {
            synchronized (SentinelRuleManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SentinelRuleManager();
                }
            }
        }
        return INSTANCE;
    }

    public ISentinelListener getListener() {
        return listener;
    }

    public void setListener(ISentinelListener listener) {
        this.listener = listener;
    }

    public Set<String> getFlowRules() {
        return this.sentinelRules.values()
                                 .stream()
                                 .flatMap(composite -> composite.flowRules.stream())
                                 .collect(Collectors.toSet());
    }

    public Set<String> getDegradingRules() {
        return this.sentinelRules.values()
                                 .stream()
                                 .flatMap(composite -> composite.degradingRules.stream())
                                 .collect(Collectors.toSet());
    }

    public void refreshFlowRules() {
        FlowRuleDto[] flowRules = ConfigurationManager.getInstance().getConfig("flowRules", FlowRuleDto[].class);

        Map<String, FlowRuleDto> configRules = Arrays.stream(flowRules)
                                                     .collect(Collectors.toMap(FlowRuleDto::getRuleId, val -> val));

        List<String> deleteRules = new ArrayList<>();
        List<FlowRuleDto> updateRules = new ArrayList<>();

        for (Map.Entry<String, FlowRuleDto> entry : flowId2Rules.entrySet()) {
            String key = entry.getKey();
            FlowRuleDto inMemoryRule = entry.getValue();

            FlowRuleDto configRule = configRules.remove(key);
            if (configRule == null) {
                // this rule in memory does not exist in the current configuration
                deleteRules.add(key);
            } else {
                if (!configRule.equals(inMemoryRule)) {
                    updateRules.add(configRule);
                }
            }
        }

        // delete rules
        if (!deleteRules.isEmpty()) {
            SentinelRuleManager.this.deleteFlowRule("Config", deleteRules, false);
        }

        List<FlowRuleDto> changed = new ArrayList<>();

        // update rules
        for (FlowRuleDto rule : updateRules) {
            try {
                rule.valid();
            } catch (SentinelCommandException e) {
                log.warn("Ignore invalid flow control rule: {}, {}",
                         rule,
                         e.getMessage());
                continue;
            }
            SentinelRuleManager.this.updateFlowRule("Config", rule, false);
            changed.add(rule);
        }

        // add rules which are left in configurations
        for (FlowRuleDto rule : configRules.values()) {
            try {
                rule.valid();
            } catch (SentinelCommandException e) {
                log.warn("Ignore invalid flow control rule: {}, {}",
                         rule,
                         e.getMessage());
                continue;
            }
            SentinelRuleManager.this.addFlowControlRule("Config", rule, false);
            changed.add(rule);
        }

        if (!changed.isEmpty() || !deleteRules.isEmpty()) {
            log.info("reload flow rules, changed:{}, deleted:{}", changed, deleteRules);

            // reload
            FlowRuleManager.loadRules(
                flowId2Rules.values()
                            .stream()
                            .map(FlowRuleDto::toFlowRule)
                            .collect(Collectors.toList()));

            SentinelRuleManager.this.listener.onFlowRuleLoaded("Config",
                                                               updateRules);
        }
    }

    public void refreshDegradingRule() {
        DegradingRuleDto[] degradingRules = ConfigurationManager.getInstance().getConfig("degradingRules", DegradingRuleDto[].class);

        Map<String, DegradingRuleDto> configRules = Arrays.stream(degradingRules)
                                                          .collect(Collectors.toMap(DegradingRuleDto::getRuleId, val -> val));

        List<String> deleted = new ArrayList<>();
        List<DegradingRuleDto> updated = new ArrayList<>();

        for (Map.Entry<String, DegradingRuleDto> entry : degradeId2Rules.entrySet()) {
            String key = entry.getKey();
            DegradingRuleDto inMemoryRule = entry.getValue();

            DegradingRuleDto configRule = configRules.remove(key);
            if (configRule == null) {
                // this rule in memory does not exist in the current configuration
                deleted.add(key);
            } else {
                if (!configRule.equals(inMemoryRule)) {
                    updated.add(configRule);
                }
            }
        }

        // delete rules
        if (!deleted.isEmpty()) {
            SentinelRuleManager.this.deleteDegradingRule("Config", deleted, false);
        }

        List<DegradingRuleDto> changed = new ArrayList<>();

        for (DegradingRuleDto rule : updated) {
            try {
                rule.valid();
            } catch (SentinelCommandException e) {
                log.warn("Ignore invalid degrade control rule: {}, {}",
                         rule,
                         e.getMessage());
                continue;
            }
            SentinelRuleManager.this.updateDegradingRule("Config", rule, false);
            changed.add(rule);
        }

        // add rules which are left in configurations
        for (DegradingRuleDto rule : configRules.values()) {
            try {
                rule.valid();
            } catch (SentinelCommandException e) {
                log.warn("Ignore invalid flow control rule: {}, {}",
                         rule,
                         e.getMessage());
                continue;
            }
            SentinelRuleManager.this.addDegradingRule("Config", rule, false);
            changed.add(rule);
        }

        // reload rules
        if (!changed.isEmpty() || !deleted.isEmpty()) {
            log.info("reload degrade rules, changed:{}, deleted:{}", changed, deleted);

            DegradeRuleManager.loadRules(
                degradeId2Rules.values()
                               .stream()
                               .map(DegradingRuleDto::toDegradeRule)
                               .collect(Collectors.toList()));

            SentinelRuleManager.this.listener.onDegradeRuleLoaded("Config", changed);
        }
    }

    public CompositeRule matches(String requestPath) {
        for (CompositeRule rule : sentinelRules.values()) {
            if (rule.urlMatcher.matches(requestPath)) {
                return rule;
            }
        }
        return null;
    }

    public void addFlowControlRule(String source, FlowRuleDto rule, boolean loadRules) {
        if (flowId2Rules.putIfAbsent(rule.getRuleId(), rule) != null) {
            log.warn("flow rule [{}] exists", rule.getRuleId());
            return;
        }

        sentinelRules.computeIfAbsent(rule.getUri(),
                                      key -> new CompositeRule(IUrlMatcher.createMatcher(rule.getUri())))
                     .addFlowRule(rule);

        if (loadRules) {
            FlowRuleManager.loadRules(
                flowId2Rules.values()
                            .stream()
                            .map(FlowRuleDto::toFlowRule)
                            .collect(Collectors.toList()));

            this.listener.onFlowRuleLoaded(source, Collections.singletonList(rule));
        }
    }

    public void updateFlowRule(String source, FlowRuleDto newRule, boolean loadRules) {
        if (!flowId2Rules.containsKey(newRule.getRuleId())) {
            throw new SentinelCommandException(String.format(Locale.ENGLISH, "flow rule[%s] not exist", newRule.getRuleId()));
        }
        deleteFlowRule(source, Collections.singletonList(newRule.getRuleId()), loadRules);
        addFlowControlRule(source, newRule, loadRules);
    }

    public void deleteFlowRule(String source, List<String> idList, boolean loadRules) {
        List<FlowRuleDto> deleteRules = new ArrayList<>();
        for (String id : idList) {
            FlowRuleDto rule = flowId2Rules.remove(id);
            if (rule != null) {
                deleteRules.add(rule);
            }
        }

        Iterator<Map.Entry<String, CompositeRule>> i = this.sentinelRules.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<String, CompositeRule> entry = i.next();
            CompositeRule rule = entry.getValue();
            rule.removeFlowRule(idList);
            if (rule.isEmpty()) {
                i.remove();
            }
        }

        if (loadRules) {
            // reload again even the deleteRules is empty to ensure the underlying configuration is the same as current
            FlowRuleManager.loadRules(
                flowId2Rules.values()
                            .stream()
                            .map(FlowRuleDto::toFlowRule)
                            .collect(Collectors.toList()));
        }
        if (!deleteRules.isEmpty()) {
            this.listener.onFlowRuleUnloaded(source, deleteRules);
        }
    }

    public void clearFlowRules(String source) {
        Collection<FlowRuleDto> deleteRules = new ArrayList<>(flowId2Rules.values());

        Iterator<Map.Entry<String, CompositeRule>> i = this.sentinelRules.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<String, CompositeRule> entry = i.next();
            CompositeRule rule = entry.getValue();
            rule.clearFlowRule();
            if (rule.isEmpty()) {
                i.remove();
            }
        }

        this.flowId2Rules.clear();
        FlowRuleManager.loadRules(Collections.emptyList());

        this.listener.onFlowRuleUnloaded(source, deleteRules);
    }

    public void addDegradingRule(String source, DegradingRuleDto rule, boolean loadRules) {
        if (degradeId2Rules.putIfAbsent(rule.getRuleId(), rule) != null) {
            log.warn("degrade rule [{}] exists", rule.getRuleId());
            return;
        }

        sentinelRules.computeIfAbsent(rule.getUri(),
                                      key -> new CompositeRule(IUrlMatcher.createMatcher(rule.getUri())))
                     .addDegradingRule(rule);

        if (loadRules) {
            DegradeRuleManager.loadRules(
                degradeId2Rules.values()
                               .stream()
                               .map(DegradingRuleDto::toDegradeRule)
                               .collect(Collectors.toList()));

            this.listener.onDegradeRuleLoaded(source, Collections.singletonList(rule));
        }
    }

    public void updateDegradingRule(String source, DegradingRuleDto newRule, boolean loadRules) {
        if (!degradeId2Rules.containsKey(newRule.getRuleId())) {
            throw new SentinelCommandException(String.format(Locale.ENGLISH, "degrade rule [%s] not exist", newRule.getRuleId()));
        }
        deleteDegradingRule(source, Collections.singletonList(newRule.getRuleId()), loadRules);
        addDegradingRule(source, newRule, loadRules);
    }

    public void deleteDegradingRule(String source, List<String> idList, boolean loadRules) {
        List<DegradingRuleDto> deleteRules = new ArrayList<>();
        for (String id : idList) {
            DegradingRuleDto rule = degradeId2Rules.remove(id);
            if (rule != null) {
                deleteRules.add(rule);
            }
        }

        Iterator<Map.Entry<String, CompositeRule>> i = this.sentinelRules.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<String, CompositeRule> entry = i.next();
            CompositeRule rule = entry.getValue();
            rule.removeDegradingRule(idList);
            if (rule.isEmpty()) {
                i.remove();
            }
        }

        if (loadRules) {
            // reload again even the deleteRules is empty to ensure the underlying configuration is the same as current
            DegradeRuleManager.loadRules(
                degradeId2Rules.values()
                               .stream()
                               .map(DegradingRuleDto::toDegradeRule)
                               .collect(Collectors.toList()));
        }
        if (!deleteRules.isEmpty()) {
            this.listener.onDegradeRuleUnloaded(source, deleteRules);
        }
    }

    public void clearDegradingRules(String source) {
        Collection<DegradingRuleDto> deleteRules = new ArrayList<>(degradeId2Rules.values());

        Iterator<Map.Entry<String, CompositeRule>> i = this.sentinelRules.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<String, CompositeRule> entry = i.next();
            CompositeRule rule = entry.getValue();
            rule.clearDegradingRule();
            if (rule.isEmpty()) {
                i.remove();
            }
        }

        degradeId2Rules.clear();

        DegradeRuleManager.loadRules(Collections.emptyList());

        this.listener.onDegradeRuleUnloaded(source, deleteRules);
    }

    /**
     * uri --- { flow-rules, degrade-rules }
     */
    public static class CompositeRule {
        private final Set<String> flowRules = new ConcurrentSkipListSet<>();
        private final Set<String> degradingRules = new ConcurrentSkipListSet<>();
        private final IUrlMatcher urlMatcher;

        CompositeRule(IUrlMatcher urlMatcher) {
            this.urlMatcher = urlMatcher;
        }

        public IUrlMatcher getUrlMatcher() {
            return urlMatcher;
        }

        public void addFlowRule(FlowRuleDto flowRule) {
            flowRules.add(flowRule.getRuleId());
        }

        public void addDegradingRule(DegradingRuleDto degradeRule) {
            degradingRules.add(degradeRule.getRuleId());
        }

        public void removeFlowRule(List<String> idList) {
            idList.forEach(flowRules::remove);
        }

        public void clearFlowRule() {
            flowRules.clear();
        }

        public void removeDegradingRule(List<String> idList) {
            idList.forEach(degradingRules::remove);
        }

        public boolean isEmpty() {
            return flowRules.isEmpty() && degradingRules.isEmpty();
        }

        public void clearDegradingRule() {
            degradingRules.clear();
        }

        public Set<String> getFlowRules() {
            return flowRules;
        }

        public Set<String> getDegradingRules() {
            return degradingRules;
        }
    }

    class FlowRuleChangedListener implements IConfigurationChangedListener {
        @Override
        public void onChange(Set<String> keys) {
            if (keys.contains("flowRules")) {
                refreshFlowRules();
            }
        }
    }

    class DegradingRuleChangedListener implements IConfigurationChangedListener {
        @Override
        public void onChange(Set<String> keys) {
            if (keys.contains("degradingRules")) {
                refreshDegradingRule();
            }
        }
    }
}
