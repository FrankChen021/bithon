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

package com.sbss.bithon.agent.sentinel;

import com.sbss.bithon.agent.sentinel.degrade.DegradeRuleDto;
import com.sbss.bithon.agent.sentinel.expt.SentinelCommandException;
import com.sbss.bithon.agent.sentinel.flow.FlowRuleDto;
import org.junit.Assert;
import org.junit.Test;
import org.bithon.shaded.com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import org.bithon.shaded.com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import org.bithon.shaded.com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import org.bithon.shaded.com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DegradeRuleTest {

    @Test
    public void duplicatedAdd() {
        ExpectedListener expectedListener = new ExpectedListener();
        SentinelRuleManager ruleManager = new SentinelRuleManager(expectedListener);
        DegradeRuleDto rule = new DegradeRuleDto();
        {
            rule.setRuleId("1");
            rule.setUri("/uri/1");
            rule.setThreshold(1.0);
            rule.setGrade(0);
            rule.setMaxResponseTime(600);
            rule.setTimeWindow(1000);
            rule.setStatIntervalMs(555);
        }
        ruleManager.addDegradeRule("Command", rule, true);

        Assert.assertEquals(1, DegradeRuleManager.getRules().size());
        DegradeRule degradeRule = DegradeRuleManager.getRules().get(0);
        Assert.assertEquals(rule.getGrade().intValue(), degradeRule.getGrade());
        Assert.assertEquals(rule.getUri(), degradeRule.getResource());
        Assert.assertEquals(rule.getMaxResponseTime().intValue(), (int) degradeRule.getCount());
        Assert.assertEquals(rule.getThreshold(), degradeRule.getSlowRatioThreshold(), 0.000001);
        Assert.assertEquals(rule.getTimeWindow().intValue(), degradeRule.getTimeWindow());
        Assert.assertEquals(rule.getStatIntervalMs().intValue(), degradeRule.getStatIntervalMs());
        Assert.assertEquals(rule, expectedListener.loadedDegradeRules.stream().findFirst().get());

        ruleManager.addDegradeRule("Command", rule, true);
    }

    @Test
    public void addGrade2() {
        ExpectedListener expectedListener = new ExpectedListener();
        SentinelRuleManager ruleManager = new SentinelRuleManager(expectedListener);
        DegradeRuleDto rule = new DegradeRuleDto();
        {
            rule.setRuleId("1");
            rule.setUri("/uri/1");
            rule.setThreshold(0.6);
            rule.setGrade(2);
            rule.setTimeWindow(1000);
            rule.setStatIntervalMs(555);
        }
        ruleManager.addDegradeRule("Command", rule, true);

        Assert.assertEquals(1, DegradeRuleManager.getRules().size());
        DegradeRule degradeRule = DegradeRuleManager.getRules().get(0);
        Assert.assertEquals(rule.getGrade().intValue(), degradeRule.getGrade());
        Assert.assertEquals(rule.getUri(), degradeRule.getResource());
        Assert.assertEquals(0.6, degradeRule.getCount(), 0.0001);
        Assert.assertEquals(1.0, degradeRule.getSlowRatioThreshold(), 0.000001);
        Assert.assertEquals(rule.getTimeWindow().intValue(), degradeRule.getTimeWindow());
        Assert.assertEquals(rule.getStatIntervalMs().intValue(), degradeRule.getStatIntervalMs());
        Assert.assertEquals(rule, expectedListener.loadedDegradeRules.stream().findFirst().get());
    }

    @Test
    public void basicTest() {
        ExpectedListener expectedListener = new ExpectedListener();
        SentinelRuleManager ruleManager = new SentinelRuleManager(expectedListener);
        DegradeRuleDto rule = new DegradeRuleDto();
        {
            rule.setRuleId("1");
            rule.setUri("/uri/1");
            rule.setThreshold(1.0);
            rule.setGrade(0);
            rule.setMaxResponseTime(600);
            rule.setTimeWindow(1000);
            rule.setStatIntervalMs(555);
        }
        ruleManager.addDegradeRule("Command", rule, true);

        Assert.assertEquals(1, DegradeRuleManager.getRules().size());
        DegradeRule degradeRule = DegradeRuleManager.getRules().get(0);
        Assert.assertEquals(rule.getGrade().intValue(), degradeRule.getGrade());
        Assert.assertEquals(rule.getUri(), degradeRule.getResource());
        Assert.assertEquals(rule.getMaxResponseTime().intValue(), (int) degradeRule.getCount());
        Assert.assertEquals(rule.getThreshold(), degradeRule.getSlowRatioThreshold(), 0.000001);
        Assert.assertEquals(rule.getTimeWindow().intValue(), degradeRule.getTimeWindow());
        Assert.assertEquals(rule.getStatIntervalMs().intValue(), degradeRule.getStatIntervalMs());
        Assert.assertEquals(rule, expectedListener.loadedDegradeRules.stream().findFirst().get());

        //
        // Delete a rule that does not exist
        //
        ruleManager.listener = new ListenerAdaptor() {
            @Override
            public void onDegradeRuleUnloaded(String source, Collection<DegradeRuleDto> rule) {
                Assert.assertEquals("This should not happen", 0, 1);
            }
        };
        ruleManager.deleteDegradeRule("Command", Collections.singletonList("2"), true);

        // no change
        Assert.assertEquals(1, DegradeRuleManager.getRules().size());
        degradeRule = DegradeRuleManager.getRules().get(0);
        Assert.assertEquals(rule.getGrade().intValue(), degradeRule.getGrade());
        Assert.assertEquals(rule.getUri(), degradeRule.getResource());
        Assert.assertEquals(rule.getMaxResponseTime().intValue(), (int) degradeRule.getCount());
        Assert.assertEquals(rule.getThreshold(), degradeRule.getSlowRatioThreshold(), 0.000001);
        Assert.assertEquals(rule.getTimeWindow().intValue(), degradeRule.getTimeWindow());
        Assert.assertEquals(rule.getStatIntervalMs().intValue(), degradeRule.getStatIntervalMs());
        Assert.assertEquals(rule, expectedListener.loadedDegradeRules.stream().findFirst().get());

        //
        // test Update
        //
        DegradeRuleDto updateNonExistingRule = new DegradeRuleDto();
        {
            updateNonExistingRule.setRuleId("2");
            updateNonExistingRule.setUri("/uri/2");
            updateNonExistingRule.setThreshold(1.0);
            updateNonExistingRule.setGrade(0);
            updateNonExistingRule.setMaxResponseTime(600);
        }
        try {
            ruleManager.updateDegradeRule("Command", updateNonExistingRule, true);
            Assert.assertEquals("Should not happen", 0, 1);
        } catch (SentinelCommandException e) {
            Assert.assertTrue(e.getMessage().contains("not exist"));
        }

        //
        // Update existing rule
        //
        DegradeRuleDto updateRule = new DegradeRuleDto();
        {
            updateRule.setRuleId("1");
            updateRule.setUri("/uri/2");
            updateRule.setThreshold(.5);
            updateRule.setGrade(0);
            updateRule.setTimeWindow(789);
            updateRule.setMaxResponseTime(700);
            updateRule.setStatIntervalMs(445);
        }
        ruleManager.listener = expectedListener;
        ruleManager.updateDegradeRule("Command", updateRule, true);
        Assert.assertEquals(1, DegradeRuleManager.getRules().size());
        degradeRule = DegradeRuleManager.getRules().get(0);
        Assert.assertEquals(updateRule.getGrade().intValue(), degradeRule.getGrade());
        Assert.assertEquals(updateRule.getUri(), degradeRule.getResource());
        Assert.assertEquals(updateRule.getMaxResponseTime().intValue(), (int) degradeRule.getCount());
        Assert.assertEquals(updateRule.getThreshold(), degradeRule.getSlowRatioThreshold(), 0.000001);
        Assert.assertEquals(updateRule.getTimeWindow().intValue(), degradeRule.getTimeWindow());
        Assert.assertEquals(updateRule.getStatIntervalMs().intValue(), degradeRule.getStatIntervalMs());
        Assert.assertEquals(updateRule, expectedListener.loadedDegradeRules.stream().findFirst().get());

        //
        // Delete existing rule
        //
        ruleManager.listener = expectedListener;
        ruleManager.deleteDegradeRule("Command", Collections.singletonList("1"), true);
        Assert.assertEquals(0, DegradeRuleManager.getRules().size());
        Assert.assertEquals(0, ruleManager.degradeId2Rules.size());

        //
        // Update again after deletion
        //
        try {
            ruleManager.updateDegradeRule("Command", updateRule, true);
            Assert.assertEquals("Should not happen", 0, 1);
        } catch (SentinelCommandException e) {
            Assert.assertTrue(e.getMessage().contains("not exist"));
        }
    }

    @Test
    public void rulesOnSameURI() {
        ExpectedListener expectedListener = new ExpectedListener();
        SentinelRuleManager ruleManager = new SentinelRuleManager(expectedListener);
        DegradeRuleDto rule1 = new DegradeRuleDto();
        {
            rule1.setRuleId("1");
            rule1.setUri("/uri/1");
            rule1.setThreshold(.5);
            rule1.setGrade(0);
            rule1.setTimeWindow(789);
            rule1.setMaxResponseTime(700);
            rule1.setStatIntervalMs(445);
        }
        ruleManager.addDegradeRule("Command", rule1, true);

        Assert.assertEquals(1, DegradeRuleManager.getRules().size());
        DegradeRule degradeRule = DegradeRuleManager.getRules().get(0);
        Assert.assertEquals(rule1.getGrade().intValue(), degradeRule.getGrade());
        Assert.assertEquals(rule1.getUri(), degradeRule.getResource());
        Assert.assertEquals(rule1.getMaxResponseTime().intValue(), (int) degradeRule.getCount());
        Assert.assertEquals(rule1.getThreshold(), degradeRule.getSlowRatioThreshold(), 0.000001);
        Assert.assertEquals(rule1.getTimeWindow().intValue(), degradeRule.getTimeWindow());
        Assert.assertEquals(rule1.getStatIntervalMs().intValue(), degradeRule.getStatIntervalMs());
        Assert.assertEquals(rule1, expectedListener.loadedDegradeRules.stream().findFirst().get());

        //
        // add 2 rule
        //
        DegradeRuleDto rule2 = new DegradeRuleDto();
        {
            rule2.setRuleId("2");
            rule2.setUri("/uri/1");
            rule2.setThreshold(.5);
            rule2.setGrade(0);
            rule2.setTimeWindow(7000);
            rule2.setMaxResponseTime(700);
            rule2.setStatIntervalMs(445);
        }
        ruleManager.addDegradeRule("Command", rule2, true);
        Assert.assertEquals(2, DegradeRuleManager.getRules().size());
        degradeRule = DegradeRuleManager.getRules().get(0);
        Assert.assertEquals(rule1.getGrade().intValue(), degradeRule.getGrade());
        Assert.assertEquals(rule1.getUri(), degradeRule.getResource());
        Assert.assertEquals(rule1.getMaxResponseTime().intValue(), (int) degradeRule.getCount());
        Assert.assertEquals(rule1.getThreshold(), degradeRule.getSlowRatioThreshold(), 0.000001);
        Assert.assertEquals(rule1.getTimeWindow().intValue(), degradeRule.getTimeWindow());
        Assert.assertEquals(rule1.getStatIntervalMs().intValue(), degradeRule.getStatIntervalMs());

        degradeRule = DegradeRuleManager.getRules().get(1);
        Assert.assertEquals(rule2.getGrade().intValue(), degradeRule.getGrade());
        Assert.assertEquals(rule2.getUri(), degradeRule.getResource());
        Assert.assertEquals(rule2.getMaxResponseTime().intValue(), (int) degradeRule.getCount());
        Assert.assertEquals(rule2.getThreshold(), degradeRule.getSlowRatioThreshold(), 0.000001);
        Assert.assertEquals(rule2.getTimeWindow().intValue(), degradeRule.getTimeWindow());
        Assert.assertEquals(rule2.getStatIntervalMs().intValue(), degradeRule.getStatIntervalMs());
        Assert.assertEquals(rule2, expectedListener.loadedDegradeRules.stream().findFirst().get());
        Assert.assertEquals(1, ruleManager.sentinelRules.size());

        // Delete all
        ruleManager.deleteDegradeRule("Command", Arrays.asList("1", "2"), true);
        Assert.assertEquals(0, DegradeRuleManager.getRules().size());
        Assert.assertEquals(0, ruleManager.flowId2Rules.size());
        Assert.assertEquals(0, ruleManager.degradeId2Rules.size());
        Assert.assertEquals(0, ruleManager.sentinelRules.size());
        Assert.assertEquals(2, expectedListener.unLoadedDegradeRules.size());
    }

    @Test
    public void repeatedAddAndDelete() {
        ExpectedListener expectedListener = new ExpectedListener();
        SentinelRuleManager ruleManager = new SentinelRuleManager(expectedListener);

        for (int i = 0; i < 10; i++) {
            DegradeRuleDto rule1 = new DegradeRuleDto();
            {
                rule1.setRuleId("1");
                rule1.setUri("/uri/1");
                rule1.setThreshold(.5);
                rule1.setGrade(0);
                rule1.setTimeWindow(789);
                rule1.setMaxResponseTime(700);
                rule1.setStatIntervalMs(445);
            }
            ruleManager.addDegradeRule("Command", rule1, true);

            Assert.assertEquals(1, DegradeRuleManager.getRules().size());
            DegradeRule degradeRule = DegradeRuleManager.getRules().get(0);
            Assert.assertEquals(rule1.getGrade().intValue(), degradeRule.getGrade());
            Assert.assertEquals(rule1.getUri(), degradeRule.getResource());
            Assert.assertEquals(rule1.getMaxResponseTime().intValue(), (int) degradeRule.getCount());
            Assert.assertEquals(rule1.getThreshold(), degradeRule.getSlowRatioThreshold(), 0.000001);
            Assert.assertEquals(rule1.getTimeWindow().intValue(), degradeRule.getTimeWindow());
            Assert.assertEquals(rule1.getStatIntervalMs().intValue(), degradeRule.getStatIntervalMs());
            Assert.assertEquals(rule1, expectedListener.loadedDegradeRules.stream().findFirst().get());

            //
            // add 2 rule
            //
            DegradeRuleDto rule2 = new DegradeRuleDto();
            {
                rule2.setRuleId("2");
                rule2.setUri("/uri/1");
                rule2.setThreshold(.5);
                rule2.setGrade(0);
                rule2.setTimeWindow(7000);
                rule2.setMaxResponseTime(700);
                rule2.setStatIntervalMs(445);
            }
            ruleManager.addDegradeRule("Command", rule2, true);
            Assert.assertEquals(2, DegradeRuleManager.getRules().size());
            degradeRule = DegradeRuleManager.getRules().get(0);
            Assert.assertEquals(rule1.getGrade().intValue(), degradeRule.getGrade());
            Assert.assertEquals(rule1.getUri(), degradeRule.getResource());
            Assert.assertEquals(rule1.getMaxResponseTime().intValue(), (int) degradeRule.getCount());
            Assert.assertEquals(rule1.getThreshold(), degradeRule.getSlowRatioThreshold(), 0.000001);
            Assert.assertEquals(rule1.getTimeWindow().intValue(), degradeRule.getTimeWindow());
            Assert.assertEquals(rule1.getStatIntervalMs().intValue(), degradeRule.getStatIntervalMs());

            degradeRule = DegradeRuleManager.getRules().get(1);
            Assert.assertEquals(rule2.getGrade().intValue(), degradeRule.getGrade());
            Assert.assertEquals(rule2.getUri(), degradeRule.getResource());
            Assert.assertEquals(rule2.getMaxResponseTime().intValue(), (int) degradeRule.getCount());
            Assert.assertEquals(rule2.getThreshold(), degradeRule.getSlowRatioThreshold(), 0.000001);
            Assert.assertEquals(rule2.getTimeWindow().intValue(), degradeRule.getTimeWindow());
            Assert.assertEquals(rule2.getStatIntervalMs().intValue(), degradeRule.getStatIntervalMs());
            Assert.assertEquals(rule2, expectedListener.loadedDegradeRules.stream().findFirst().get());
            Assert.assertEquals(1, ruleManager.sentinelRules.size());

            // Delete all
            ruleManager.deleteDegradeRule("Command", Arrays.asList("1", "2"), true);
            Assert.assertEquals(0, DegradeRuleManager.getRules().size());
            Assert.assertEquals(0, ruleManager.flowId2Rules.size());
            Assert.assertEquals(0, ruleManager.degradeId2Rules.size());
            Assert.assertEquals(0, ruleManager.sentinelRules.size());
            Assert.assertEquals(2, expectedListener.unLoadedDegradeRules.size());
        }
    }

    @Test
    public void flowRuleAndDegradeRule() {
        ExpectedListener expectedListener = new ExpectedListener();
        SentinelRuleManager ruleManager = new SentinelRuleManager(expectedListener);
        DegradeRuleDto rule1 = new DegradeRuleDto();
        {
            rule1.setRuleId("1");
            rule1.setUri("/uri/1");
            rule1.setThreshold(.5);
            rule1.setGrade(0);
            rule1.setTimeWindow(789);
            rule1.setMaxResponseTime(700);
            rule1.setStatIntervalMs(445);
        }
        ruleManager.addDegradeRule("Command", rule1, true);

        Assert.assertEquals(1, DegradeRuleManager.getRules().size());
        DegradeRule degradeRule = DegradeRuleManager.getRules().get(0);
        Assert.assertEquals(rule1.getGrade().intValue(), degradeRule.getGrade());
        Assert.assertEquals(rule1.getUri(), degradeRule.getResource());
        Assert.assertEquals(rule1.getMaxResponseTime().intValue(), (int) degradeRule.getCount());
        Assert.assertEquals(rule1.getThreshold(), degradeRule.getSlowRatioThreshold(), 0.000001);
        Assert.assertEquals(rule1.getTimeWindow().intValue(), degradeRule.getTimeWindow());
        Assert.assertEquals(rule1.getStatIntervalMs().intValue(), degradeRule.getStatIntervalMs());
        Assert.assertEquals(rule1, expectedListener.loadedDegradeRules.stream().findFirst().get());

        //
        // add a flow with same id and uri
        //
        FlowRuleDto rule2 = new FlowRuleDto();
        {
            rule2.setRuleId("1");
            rule2.setUri("/uri/1");
            rule2.setControlBehavior(0);
            rule2.setThreshold(100);
            rule2.setGrade(0);
            rule2.setMaxTimeoutMs(500);
        }
        ruleManager.addFlowControlRule("Command", rule2, true);
        Assert.assertEquals(1, FlowRuleManager.getRules().size());
        FlowRule flowRule = FlowRuleManager.getRules().get(0);
        Assert.assertEquals(rule2.getGrade().intValue(), flowRule.getGrade());
        Assert.assertEquals(rule2.getUri(), flowRule.getResource());
        Assert.assertEquals(rule2.getThreshold(), flowRule.getCount(), 0.000001);
        Assert.assertEquals(rule2.getControlBehavior().intValue(), flowRule.getControlBehavior());
        Assert.assertEquals(rule2.getMaxTimeoutMs().intValue(), flowRule.getMaxQueueingTimeMs());
        Assert.assertEquals(rule2, expectedListener.loadedFlowRules.stream().findFirst().get());

        //
        // add a flow with different uri
        //
        FlowRuleDto rule3 = new FlowRuleDto();
        {
            rule3.setRuleId("2");
            rule3.setUri("/uri/2");
            rule3.setControlBehavior(0);
            rule3.setThreshold(100);
            rule3.setGrade(0);
            rule3.setMaxTimeoutMs(500);
        }
        ruleManager.addFlowControlRule("Command", rule3, true);
        Assert.assertEquals(2, FlowRuleManager.getRules().size());
        flowRule = FlowRuleManager.getRules().get(1);
        Assert.assertEquals(rule3.getGrade().intValue(), flowRule.getGrade());
        Assert.assertEquals(rule3.getUri(), flowRule.getResource());
        Assert.assertEquals(rule3.getThreshold(), flowRule.getCount(), 0.000001);
        Assert.assertEquals(rule3.getControlBehavior().intValue(), flowRule.getControlBehavior());
        Assert.assertEquals(rule3.getMaxTimeoutMs().intValue(), flowRule.getMaxQueueingTimeMs());
        Assert.assertEquals(rule3, expectedListener.loadedFlowRules.stream().findFirst().get());

        Assert.assertEquals(2, ruleManager.flowId2Rules.size());
        Assert.assertEquals(1, ruleManager.degradeId2Rules.size());
        Assert.assertEquals(2, ruleManager.sentinelRules.size());

        // delete degrade rule
        ruleManager.deleteDegradeRule("Command", Collections.singletonList("1"), true);
        Assert.assertEquals(rule1, expectedListener.unLoadedDegradeRules.stream().findFirst().get());
        Assert.assertEquals(null, expectedListener.unLoadedFlowRules);
        Assert.assertEquals(2, ruleManager.flowId2Rules.size());
        Assert.assertEquals(0, ruleManager.degradeId2Rules.size());
        Assert.assertEquals(2, ruleManager.sentinelRules.size());

        ruleManager.deleteFlowControlRule("Command", Collections.singletonList("1"), true);
        Assert.assertEquals(rule1, expectedListener.unLoadedDegradeRules.stream().findFirst().get());
        Assert.assertEquals(1, ruleManager.flowId2Rules.size());
        Assert.assertEquals(0, ruleManager.degradeId2Rules.size());
        Assert.assertEquals(1, ruleManager.sentinelRules.size());

        // after deletion of rule2, rule3 is still in effect
        flowRule = FlowRuleManager.getRules().get(0);
        Assert.assertEquals(rule3.getGrade().intValue(), flowRule.getGrade());
        Assert.assertEquals(rule3.getUri(), flowRule.getResource());
        Assert.assertEquals(rule3.getThreshold(), flowRule.getCount(), 0.000001);
        Assert.assertEquals(rule3.getControlBehavior().intValue(), flowRule.getControlBehavior());
        Assert.assertEquals(rule3.getMaxTimeoutMs().intValue(), flowRule.getMaxQueueingTimeMs());

        ruleManager.deleteFlowControlRule("Command", Collections.singletonList("2"), true);
        Assert.assertEquals(rule3, expectedListener.unLoadedFlowRules.stream().findFirst().get());
        Assert.assertEquals(0, ruleManager.flowId2Rules.size());
        Assert.assertEquals(0, ruleManager.degradeId2Rules.size());
        Assert.assertEquals(0, ruleManager.sentinelRules.size());
    }

    @Test
    public void clearCommand() {
        ExpectedListener expectedListener = new ExpectedListener();
        SentinelRuleManager ruleManager = new SentinelRuleManager(expectedListener);
        DegradeRuleDto rule1 = new DegradeRuleDto();
        {
            rule1.setRuleId("1");
            rule1.setUri("/uri/1");
            rule1.setThreshold(.5);
            rule1.setGrade(0);
            rule1.setTimeWindow(789);
            rule1.setMaxResponseTime(700);
            rule1.setStatIntervalMs(445);
        }
        ruleManager.addDegradeRule("Command", rule1, true);

        Assert.assertEquals(1, DegradeRuleManager.getRules().size());
        DegradeRule degradeRule = DegradeRuleManager.getRules().get(0);
        Assert.assertEquals(rule1.getGrade().intValue(), degradeRule.getGrade());
        Assert.assertEquals(rule1.getUri(), degradeRule.getResource());
        Assert.assertEquals(rule1.getMaxResponseTime().intValue(), (int) degradeRule.getCount());
        Assert.assertEquals(rule1.getThreshold(), degradeRule.getSlowRatioThreshold(), 0.000001);
        Assert.assertEquals(rule1.getTimeWindow().intValue(), degradeRule.getTimeWindow());
        Assert.assertEquals(rule1.getStatIntervalMs().intValue(), degradeRule.getStatIntervalMs());
        Assert.assertEquals(rule1, expectedListener.loadedDegradeRules.stream().findFirst().get());

        DegradeRuleDto degradeRule2 = new DegradeRuleDto();
        {
            degradeRule2.setRuleId("456");
            degradeRule2.setUri("/uri/3");
            degradeRule2.setThreshold(.5);
            degradeRule2.setGrade(0);
            degradeRule2.setTimeWindow(789);
            degradeRule2.setMaxResponseTime(700);
            degradeRule2.setStatIntervalMs(445);
        }
        ruleManager.addDegradeRule("Command", degradeRule2, true);

        //
        // add a flow with same id and uri
        //
        FlowRuleDto rule2 = new FlowRuleDto();
        {
            rule2.setRuleId("1");
            rule2.setUri("/uri/1");
            rule2.setControlBehavior(0);
            rule2.setThreshold(100);
            rule2.setGrade(0);
            rule2.setMaxTimeoutMs(500);
        }
        ruleManager.addFlowControlRule("Command", rule2, true);
        Assert.assertEquals(1, FlowRuleManager.getRules().size());
        FlowRule flowRule = FlowRuleManager.getRules().get(0);
        Assert.assertEquals(rule2.getGrade().intValue(), flowRule.getGrade());
        Assert.assertEquals(rule2.getUri(), flowRule.getResource());
        Assert.assertEquals(rule2.getThreshold(), flowRule.getCount(), 0.000001);
        Assert.assertEquals(rule2.getControlBehavior().intValue(), flowRule.getControlBehavior());
        Assert.assertEquals(rule2.getMaxTimeoutMs().intValue(), flowRule.getMaxQueueingTimeMs());
        Assert.assertEquals(rule2, expectedListener.loadedFlowRules.stream().findFirst().get());

        //
        // add a flow with different uri
        //
        FlowRuleDto rule3 = new FlowRuleDto();
        {
            rule3.setRuleId("2");
            rule3.setUri("/uri/2");
            rule3.setControlBehavior(0);
            rule3.setThreshold(100);
            rule3.setGrade(0);
            rule3.setMaxTimeoutMs(500);
        }
        ruleManager.addFlowControlRule("Command", rule3, true);
        Assert.assertEquals(2, FlowRuleManager.getRules().size());
        flowRule = FlowRuleManager.getRules().get(1);
        Assert.assertEquals(rule3.getGrade().intValue(), flowRule.getGrade());
        Assert.assertEquals(rule3.getUri(), flowRule.getResource());
        Assert.assertEquals(rule3.getThreshold(), flowRule.getCount(), 0.000001);
        Assert.assertEquals(rule3.getControlBehavior().intValue(), flowRule.getControlBehavior());
        Assert.assertEquals(rule3.getMaxTimeoutMs().intValue(), flowRule.getMaxQueueingTimeMs());
        Assert.assertEquals(rule3, expectedListener.loadedFlowRules.stream().findFirst().get());

        Assert.assertEquals(2, ruleManager.flowId2Rules.size());
        Assert.assertEquals(2, ruleManager.degradeId2Rules.size());
        Assert.assertEquals(3, ruleManager.sentinelRules.size());

        ruleManager.clearDegradeRules("Command");
        Assert.assertEquals(2, ruleManager.flowId2Rules.size());
        Assert.assertEquals(0, ruleManager.degradeId2Rules.size());
        Assert.assertEquals(2, ruleManager.sentinelRules.size());

        ruleManager.clearFlowRules("Command");
        Assert.assertEquals(0, ruleManager.flowId2Rules.size());
        Assert.assertEquals(0, ruleManager.degradeId2Rules.size());
        Assert.assertEquals(0, ruleManager.sentinelRules.size());

        ruleManager.addDegradeRule("Command", rule1, true);
        ruleManager.addFlowControlRule("Command", rule3, true);
        Assert.assertEquals(1, ruleManager.flowId2Rules.size());
        Assert.assertEquals(1, ruleManager.degradeId2Rules.size());
        Assert.assertEquals(2, ruleManager.sentinelRules.size());
    }

    @Test
    public void testConsistencyCheck() {
        ExpectedListener expectedListener = new ExpectedListener();
        SentinelRuleManager ruleManager = new SentinelRuleManager(expectedListener);
        DegradeRuleDto rule1 = new DegradeRuleDto();
        {
            rule1.setRuleId("1");
            rule1.setUri("/uri/1");
            rule1.setThreshold(.5);
            rule1.setGrade(0);
            rule1.setTimeWindow(789);
            rule1.setMaxResponseTime(700);
            rule1.setStatIntervalMs(445);
        }
        DegradeRuleDto rule2 = new DegradeRuleDto();
        {
            rule2.setRuleId("2");
            rule2.setUri("/uri/2");
            rule2.setThreshold(.5);
            rule2.setGrade(0);
            rule2.setTimeWindow(789);
            rule2.setMaxResponseTime(700);
            rule2.setStatIntervalMs(445);
        }
        DegradeRuleDto rule3 = new DegradeRuleDto();
        {
            rule3.setRuleId("3");
            rule3.setUri("/uri/3");
            rule3.setThreshold(.5);
            rule3.setGrade(0);
            rule3.setTimeWindow(789);
            rule3.setMaxResponseTime(700);
            rule3.setStatIntervalMs(445);
        }
        ruleManager.addDegradeRule("Command", rule1, true);
        ruleManager.addDegradeRule("Command", rule2, true);
        ruleManager.addDegradeRule("Command", rule3, true);

        Assert.assertEquals(3, DegradeRuleManager.getRules().size());

        Map<String, DegradeRuleDto> config = new HashMap<>();

        DegradeRuleDto rule4 = new DegradeRuleDto();
        {
            rule4.setRuleId("4");
            rule4.setUri("/uri/4");
            rule4.setThreshold(.5);
            rule4.setGrade(0);
            rule4.setTimeWindow(789);
            rule4.setMaxResponseTime(700);
            rule4.setStatIntervalMs(445);
        }
        DegradeRuleDto rule1Changed = new DegradeRuleDto();
        rule1Changed.setRuleId("1");
        rule1Changed.setUri("/uri/1changed");
        rule1Changed.setThreshold(.5);
        rule1Changed.setGrade(0);
        rule1Changed.setTimeWindow(789);
        rule1Changed.setMaxResponseTime(700);
        rule1Changed.setStatIntervalMs(445);

        config.put("1", rule1Changed);
        config.put("2", rule2);
        config.put("4", rule4);
        ruleManager.checkDegradeRule(config);

        Assert.assertEquals(3, DegradeRuleManager.getRules().size());

        DegradeRule degradeRule = DegradeRuleManager.getRules().get(0);
        Assert.assertEquals(rule2.getGrade().intValue(), degradeRule.getGrade());
        Assert.assertEquals(rule2.getUri(), degradeRule.getResource());
        Assert.assertEquals(rule2.getMaxResponseTime().intValue(), (int) degradeRule.getCount());
        Assert.assertEquals(rule2.getThreshold(), degradeRule.getSlowRatioThreshold(), 0.000001);
        Assert.assertEquals(rule2.getTimeWindow().intValue(), degradeRule.getTimeWindow());
        Assert.assertEquals(rule2.getStatIntervalMs().intValue(), degradeRule.getStatIntervalMs());

        degradeRule = DegradeRuleManager.getRules().get(1);
        Assert.assertEquals(rule1Changed.getGrade().intValue(), degradeRule.getGrade());
        Assert.assertEquals(rule1Changed.getUri(), degradeRule.getResource());
        Assert.assertEquals(rule1Changed.getMaxResponseTime().intValue(), (int) degradeRule.getCount());
        Assert.assertEquals(rule1Changed.getThreshold(), degradeRule.getSlowRatioThreshold(), 0.000001);
        Assert.assertEquals(rule1Changed.getTimeWindow().intValue(), degradeRule.getTimeWindow());
        Assert.assertEquals(rule1Changed.getStatIntervalMs().intValue(), degradeRule.getStatIntervalMs());

        degradeRule = DegradeRuleManager.getRules().get(2);
        Assert.assertEquals(rule4.getGrade().intValue(), degradeRule.getGrade());
        Assert.assertEquals(rule4.getUri(), degradeRule.getResource());
        Assert.assertEquals(rule4.getMaxResponseTime().intValue(), (int) degradeRule.getCount());
        Assert.assertEquals(rule4.getThreshold(), degradeRule.getSlowRatioThreshold(), 0.000001);
        Assert.assertEquals(rule4.getTimeWindow().intValue(), degradeRule.getTimeWindow());
        Assert.assertEquals(rule4.getStatIntervalMs().intValue(), degradeRule.getStatIntervalMs());

        SentinelRuleManager.CompositeRule cr = ruleManager.matches(rule1Changed.getUri());
        Assert.assertEquals(1, cr.getDegradeRules().size());
        SentinelRuleManager.CompositeRule cr2 = ruleManager.matches(rule2.getUri());
        Assert.assertEquals(1, cr2.getDegradeRules().size());
        SentinelRuleManager.CompositeRule cr4 = ruleManager.matches(rule4.getUri());
        Assert.assertEquals(1, cr4.getDegradeRules().size());
        SentinelRuleManager.CompositeRule cr3 = ruleManager.matches(rule3.getUri());
        Assert.assertNull(cr3);
    }
}
