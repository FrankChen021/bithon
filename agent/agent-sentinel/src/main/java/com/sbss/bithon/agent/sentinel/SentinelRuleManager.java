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

package com.sbss.bithon.agent.sentinel;

import com.sbss.bithon.agent.controller.setting.AgentSettingManager;
import com.sbss.bithon.agent.controller.setting.IAgentSettingRefreshListener;
import com.sbss.bithon.agent.sentinel.degrade.DegradeRuleDto;
import com.sbss.bithon.agent.sentinel.expt.SentinelCommandException;
import com.sbss.bithon.agent.sentinel.flow.FlowRuleDto;
import shaded.com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import shaded.com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import shaded.com.fasterxml.jackson.databind.JsonNode;
import shaded.com.fasterxml.jackson.databind.ObjectMapper;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

/**
 * @author frankchen
 */
public class SentinelRuleManager {

    private static final Logger log = LoggerFactory.getLogger(SentinelRuleManager.class);
    private static SentinelRuleManager INSTANCE;

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
    final Map<String, DegradeRuleDto> degradeId2Rules = new ConcurrentHashMap<>();
    ISentinelListener listener;

    /**
     * Only used for unit tests
     */
    public SentinelRuleManager(ISentinelListener listener) {
        this.listener = listener;
    }

    private SentinelRuleManager() {
        AgentSettingManager manager = AgentSettingManager.getInstance();
        FlowRuleRefreshListener flowRuleListener = new FlowRuleRefreshListener();
        manager.register("flowRules", flowRuleListener);

        DegradeRuleRefreshListener degradeRuleListener = new DegradeRuleRefreshListener();
        manager.register("degradeRules", degradeRuleListener);

        Map<String, JsonNode> latestSettings = AgentSettingManager.getInstance().getLatestSettings();
        JsonNode flowRuleNodes = latestSettings.get("flowRules");
        if (flowRuleNodes != null) {
            flowRuleListener.onRefresh(manager.getObjectMapper(), flowRuleNodes);
        }
        JsonNode degradingRuleNodes = latestSettings.get("degradeRules");
        if (degradingRuleNodes != null) {
            degradeRuleListener.onRefresh(manager.getObjectMapper(), degradingRuleNodes);
        }
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

    public Set<String> getDegradeRules() {
        return this.sentinelRules.values()
                                 .stream()
                                 .flatMap(composite -> composite.degradeRules.stream())
                                 .collect(Collectors.toSet());
    }

    public void checkFlowRule(Map<String, FlowRuleDto> configRules) {
        List<String> deleteRules = new ArrayList<>();
        List<FlowRuleDto> updateRules = new ArrayList<>();

        for (Map.Entry<String, FlowRuleDto> entry : flowId2Rules.entrySet()) {
            String key = entry.getKey();
            FlowRuleDto inMemoryRule = entry.getValue();

            FlowRuleDto configRule = configRules.remove(key);
            if (configRule == null) {
                // this rule in memory does not exist in current configuration
                deleteRules.add(key);
            } else {
                if (!configRule.equals(inMemoryRule)) {
                    updateRules.add(configRule);
                }
            }
        }

        // delete rules
        if (!deleteRules.isEmpty()) {
            SentinelRuleManager.this.deleteFlowControlRule("Config", deleteRules, false);
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
            SentinelRuleManager.this.updateFlowControlRule("Config", rule, false);
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

    public void checkDegradeRule(Map<String, DegradeRuleDto> configRules) {
        List<String> deleted = new ArrayList<>();
        List<DegradeRuleDto> updated = new ArrayList<>();

        for (Map.Entry<String, DegradeRuleDto> entry : degradeId2Rules.entrySet()) {
            String key = entry.getKey();
            DegradeRuleDto inMemoryRule = entry.getValue();

            DegradeRuleDto configRule = configRules.remove(key);
            if (configRule == null) {
                // this rule in memory does not exist in current configuration
                deleted.add(key);
            } else {
                if (!configRule.equals(inMemoryRule)) {
                    updated.add(configRule);
                }
            }
        }

        // delete rules
        if (!deleted.isEmpty()) {
            SentinelRuleManager.this.deleteDegradeRule("Config", deleted, false);
        }

        List<DegradeRuleDto> changed = new ArrayList<>();

        for (DegradeRuleDto rule : updated) {
            try {
                rule.valid();
            } catch (SentinelCommandException e) {
                log.warn("Ignore invalid degrade control rule: {}, {}",
                         rule,
                         e.getMessage());
                continue;
            }
            SentinelRuleManager.this.updateDegradeRule("Config", rule, false);
            changed.add(rule);
        }

        // add rules which are left in configurations
        for (DegradeRuleDto rule : configRules.values()) {
            try {
                rule.valid();
            } catch (SentinelCommandException e) {
                log.warn("Ignore invalid flow control rule: {}, {}",
                         rule,
                         e.getMessage());
                continue;
            }
            SentinelRuleManager.this.addDegradeRule("Config", rule, false);
            changed.add(rule);
        }

        // reload rules
        if (!changed.isEmpty() || !deleted.isEmpty()) {
            log.info("reload degrade rules, changed:{}, deleted:{}", changed, deleted);

            DegradeRuleManager.loadRules(
                degradeId2Rules.values()
                               .stream()
                               .map(DegradeRuleDto::toDegradeRule)
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

    public void updateFlowControlRule(String source, FlowRuleDto newRule, boolean loadRules) {
        if (!flowId2Rules.containsKey(newRule.getRuleId())) {
            throw new SentinelCommandException(String.format("flow rule[%s] not exist", newRule.getRuleId()));
        }
        deleteFlowControlRule(source, Collections.singletonList(newRule.getRuleId()), loadRules);
        addFlowControlRule(source, newRule, loadRules);
    }

    public void deleteFlowControlRule(String source, List<String> idList, boolean loadRules) {
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

    public void addDegradeRule(String source, DegradeRuleDto rule, boolean loadRules) {
        if (degradeId2Rules.putIfAbsent(rule.getRuleId(), rule) != null) {
            log.warn("degrade rule [{}] exists", rule.getRuleId());
            return;
        }

        sentinelRules.computeIfAbsent(rule.getUri(),
                                      key -> new CompositeRule(IUrlMatcher.createMatcher(rule.getUri())))
                     .addDegradeRule(rule);

        if (loadRules) {
            DegradeRuleManager.loadRules(
                degradeId2Rules.values()
                               .stream()
                               .map(DegradeRuleDto::toDegradeRule)
                               .collect(Collectors.toList()));

            this.listener.onDegradeRuleLoaded(source, Collections.singletonList(rule));
        }
    }

    public void updateDegradeRule(String source, DegradeRuleDto newRule, boolean loadRules) {
        if (!degradeId2Rules.containsKey(newRule.getRuleId())) {
            throw new SentinelCommandException(String.format("degrade rule [%s] not exist", newRule.getRuleId()));
        }
        deleteDegradeRule(source, Collections.singletonList(newRule.getRuleId()), loadRules);
        addDegradeRule(source, newRule, loadRules);
    }

    public void deleteDegradeRule(String source, List<String> idList, boolean loadRules) {
        List<DegradeRuleDto> deleteRules = new ArrayList<>();
        for (String id : idList) {
            DegradeRuleDto rule = degradeId2Rules.remove(id);
            if (rule != null) {
                deleteRules.add(rule);
            }
        }

        Iterator<Map.Entry<String, CompositeRule>> i = this.sentinelRules.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<String, CompositeRule> entry = i.next();
            CompositeRule rule = entry.getValue();
            rule.removeDegradeRule(idList);
            if (rule.isEmpty()) {
                i.remove();
            }
        }

        if (loadRules) {
            // reload again even the deleteRules is empty to ensure the underlying configuration is the same as current
            DegradeRuleManager.loadRules(
                degradeId2Rules.values()
                               .stream()
                               .map(DegradeRuleDto::toDegradeRule)
                               .collect(Collectors.toList()));
        }
        if (!deleteRules.isEmpty()) {
            this.listener.onDegradeRuleUnloaded(source, deleteRules);
        }
    }

    public void clearDegradeRules(String source) {
        Collection<DegradeRuleDto> deleteRules = new ArrayList<>(degradeId2Rules.values());

        Iterator<Map.Entry<String, CompositeRule>> i = this.sentinelRules.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<String, CompositeRule> entry = i.next();
            CompositeRule rule = entry.getValue();
            rule.clearDegradeRule();
            if (rule.isEmpty()) {
                i.remove();
            }
        }

        degradeId2Rules.clear();

        DegradeRuleManager.loadRules(Collections.emptyList());

        this.listener.onDegradeRuleUnloaded(source, deleteRules);
    }

    /**
     * uri ---> { flow-rules, degrade-rules }
     */
    public static class CompositeRule {
        private final Set<String> flowRules = new ConcurrentSkipListSet<>();
        private final Set<String> degradeRules = new ConcurrentSkipListSet<>();
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

        public void addDegradeRule(DegradeRuleDto degradeRule) {
            degradeRules.add(degradeRule.getRuleId());
        }

        public void removeFlowRule(List<String> idList) {
            idList.forEach(flowRules::remove);
        }

        public void clearFlowRule() {
            flowRules.clear();
        }

        public void removeDegradeRule(List<String> idList) {
            idList.forEach(degradeRules::remove);
        }

        public boolean isEmpty() {
            return flowRules.isEmpty() && degradeRules.isEmpty();
        }

        public void clearDegradeRule() {
            degradeRules.clear();
        }

        public Set<String> getFlowRules() {
            return flowRules;
        }

        public Set<String> getDegradeRules() {
            return degradeRules;
        }
    }

    class FlowRuleRefreshListener implements IAgentSettingRefreshListener {
        @Override
        public void onRefresh(ObjectMapper om, JsonNode configNode) {
            FlowRuleDto[] flowRules = om.convertValue(configNode, FlowRuleDto[].class);
            if (flowRules != null) {
                checkFlowRule(Arrays.stream(flowRules)
                                    .collect(Collectors.toMap(FlowRuleDto::getRuleId, val -> val)));
            }
        }
    }

    class DegradeRuleRefreshListener implements IAgentSettingRefreshListener {
        @Override
        public void onRefresh(ObjectMapper om, JsonNode configNode) {
            // check degrade rules
            DegradeRuleDto[] degradeRules = om.convertValue(configNode, DegradeRuleDto[].class);
            if (degradeRules != null) {
                checkDegradeRule(Arrays.stream(degradeRules)
                                       .collect(Collectors.toMap(DegradeRuleDto::getRuleId, val -> val)));
            }
        }
    }
}
