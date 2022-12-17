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

import com.sbss.bithon.agent.sentinel.expt.SentinelCommandException;
import com.sbss.bithon.agent.sentinel.flow.FlowRuleDto;
import org.junit.Assert;
import org.junit.Test;
import org.bithon.shaded.com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import org.bithon.shaded.com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FlowRuleTest {

    @Test
    public void duplicatedAdd() {
        ExpectedListener expectedListener = new ExpectedListener();
        SentinelRuleManager ruleManager = new SentinelRuleManager(expectedListener);
        FlowRuleDto rule = new FlowRuleDto();
        {
            rule.setRuleId("1");
            rule.setUri("/uri/1");
            rule.setControlBehavior(0);
            rule.setThreshold(100);
            rule.setGrade(0);
            rule.setMaxTimeoutMs(500);
        }
        ruleManager.addFlowControlRule("Command", rule, true);

        Assert.assertEquals(1, FlowRuleManager.getRules().size());
        FlowRule flowRule = FlowRuleManager.getRules().get(0);
        Assert.assertEquals(rule.getGrade().intValue(), flowRule.getGrade());
        Assert.assertEquals(rule.getUri(), flowRule.getResource());
        Assert.assertEquals(rule.getThreshold(), flowRule.getCount(), 0.000001);
        Assert.assertEquals(rule.getControlBehavior().intValue(), flowRule.getControlBehavior());
        Assert.assertEquals(rule.getMaxTimeoutMs().intValue(), flowRule.getMaxQueueingTimeMs());
        Assert.assertEquals(rule, expectedListener.loadedFlowRules.stream().findFirst().get());

        ruleManager.addFlowControlRule("Command", rule, true);

    }

    @Test
    public void basicTest() {
        ExpectedListener expectedListener = new ExpectedListener();
        SentinelRuleManager ruleManager = new SentinelRuleManager(expectedListener);
        FlowRuleDto rule = new FlowRuleDto();
        {
            rule.setRuleId("1");
            rule.setUri("/uri/1");
            rule.setControlBehavior(0);
            rule.setThreshold(100);
            rule.setGrade(0);
            rule.setMaxTimeoutMs(500);
        }
        ruleManager.addFlowControlRule("Command", rule, true);

        Assert.assertEquals(1, FlowRuleManager.getRules().size());
        FlowRule flowRule = FlowRuleManager.getRules().get(0);
        Assert.assertEquals(rule.getGrade().intValue(), flowRule.getGrade());
        Assert.assertEquals(rule.getUri(), flowRule.getResource());
        Assert.assertEquals(rule.getThreshold(), flowRule.getCount(), 0.000001);
        Assert.assertEquals(rule.getControlBehavior().intValue(), flowRule.getControlBehavior());
        Assert.assertEquals(rule.getMaxTimeoutMs().intValue(), flowRule.getMaxQueueingTimeMs());
        Assert.assertEquals(rule, expectedListener.loadedFlowRules.stream().findFirst().get());

        Map<String, FlowRuleDto> config = new HashMap<>();
        config.put("1", rule);
        ruleManager.checkFlowRule(config);

        //
        // Delete a rule that does not exist
        //
        ruleManager.listener = new ListenerAdaptor() {
            @Override
            public void onFlowRuleUnloaded(String source, Collection<FlowRuleDto> rule) {
                Assert.assertEquals("This should not happen", 0, 1);
            }
        };
        ruleManager.deleteFlowControlRule("Command", Collections.singletonList("2"), true);

        // no change
        Assert.assertEquals(1, FlowRuleManager.getRules().size());
        flowRule = FlowRuleManager.getRules().get(0);
        Assert.assertEquals(rule.getGrade().intValue(), flowRule.getGrade());
        Assert.assertEquals(rule.getUri(), flowRule.getResource());
        Assert.assertEquals(rule.getThreshold(), flowRule.getCount(), 0.000001);
        Assert.assertEquals(rule.getControlBehavior().intValue(), flowRule.getControlBehavior());
        Assert.assertEquals(rule.getMaxTimeoutMs().intValue(), flowRule.getMaxQueueingTimeMs());
        Assert.assertEquals(rule, expectedListener.loadedFlowRules.stream().findFirst().get());

        //
        // test Update
        //
        FlowRuleDto updateNonExistingRule = new FlowRuleDto();
        {
            updateNonExistingRule.setRuleId("2");
            updateNonExistingRule.setUri("/uri/2");
            updateNonExistingRule.setControlBehavior(0);
            updateNonExistingRule.setThreshold(200);
            updateNonExistingRule.setGrade(0);
            updateNonExistingRule.setMaxTimeoutMs(500);
        }
        try {
            ruleManager.updateFlowControlRule("Command", updateNonExistingRule, true);
            Assert.assertEquals("Should not happen", 0, 1);
        } catch (SentinelCommandException e) {
            Assert.assertTrue(e.getMessage().contains("not exist"));
        }

        //
        // Update existing rule
        //
        FlowRuleDto updateRule = new FlowRuleDto();
        {
            updateRule.setRuleId("1");
            updateRule.setUri("/uri/2");
            updateRule.setControlBehavior(0);
            updateRule.setThreshold(200);
            updateRule.setGrade(0);
            updateRule.setMaxTimeoutMs(500);
        }
        ruleManager.listener = expectedListener;
        ruleManager.updateFlowControlRule("Command", updateRule, true);
        Assert.assertEquals(1, FlowRuleManager.getRules().size());
        flowRule = FlowRuleManager.getRules().get(0);
        Assert.assertEquals(updateRule.getGrade().intValue(), flowRule.getGrade());
        Assert.assertEquals(updateRule.getUri(), flowRule.getResource());
        Assert.assertEquals(updateRule.getThreshold(), flowRule.getCount(), 0.000001);
        Assert.assertEquals(updateRule.getControlBehavior().intValue(), flowRule.getControlBehavior());
        Assert.assertEquals(updateRule.getMaxTimeoutMs().intValue(), flowRule.getMaxQueueingTimeMs());
        Assert.assertEquals(updateRule, expectedListener.loadedFlowRules.stream().findFirst().get());

        //
        // Delete existing rule
        //
        ruleManager.listener = expectedListener;
        ruleManager.deleteFlowControlRule("Command", Collections.singletonList("1"), true);
        Assert.assertEquals(0, FlowRuleManager.getRules().size());
        Assert.assertEquals(0, ruleManager.flowId2Rules.size());

        //
        // Update again after deletion
        //
        try {
            ruleManager.updateFlowControlRule("Command", updateRule, true);
            Assert.assertEquals("Should not happen", 0, 1);
        } catch (SentinelCommandException e) {
            Assert.assertTrue(e.getMessage().contains("not exist"));
        }
    }

    @Test
    public void rulesOnSameURI() {
        ExpectedListener expectedListener = new ExpectedListener();
        SentinelRuleManager ruleManager = new SentinelRuleManager(expectedListener);
        FlowRuleDto rule1 = new FlowRuleDto();
        {
            rule1.setRuleId("1");
            rule1.setUri("/uri/1");
            rule1.setControlBehavior(0);
            rule1.setThreshold(100);
            rule1.setGrade(0);
            rule1.setMaxTimeoutMs(500);
        }
        ruleManager.addFlowControlRule("Command", rule1, true);

        Assert.assertEquals(1, FlowRuleManager.getRules().size());
        FlowRule flowRule = FlowRuleManager.getRules().get(0);
        Assert.assertEquals(rule1.getGrade().intValue(), flowRule.getGrade());
        Assert.assertEquals(rule1.getUri(), flowRule.getResource());
        Assert.assertEquals(rule1.getThreshold(), flowRule.getCount(), 0.000001);
        Assert.assertEquals(rule1.getControlBehavior().intValue(), flowRule.getControlBehavior());
        Assert.assertEquals(rule1.getMaxTimeoutMs().intValue(), flowRule.getMaxQueueingTimeMs());
        Assert.assertEquals(rule1, expectedListener.loadedFlowRules.stream().findFirst().get());

        FlowRuleDto rule2 = new FlowRuleDto();
        {
            rule2.setRuleId("2");
            rule2.setUri("/uri/1");
            rule2.setControlBehavior(0);
            rule2.setThreshold(200);
            rule2.setGrade(0);
            rule2.setMaxTimeoutMs(500);
        }
        ruleManager.addFlowControlRule("Command", rule2, true);
        Assert.assertEquals(2, FlowRuleManager.getRules().size());
        flowRule = FlowRuleManager.getRules().get(0);
        Assert.assertEquals(rule1.getGrade().intValue(), flowRule.getGrade());
        Assert.assertEquals(rule1.getUri(), flowRule.getResource());
        Assert.assertEquals(rule1.getThreshold(), flowRule.getCount(), 0.000001);
        Assert.assertEquals(rule1.getControlBehavior().intValue(), flowRule.getControlBehavior());
        Assert.assertEquals(rule1.getMaxTimeoutMs().intValue(), flowRule.getMaxQueueingTimeMs());
        flowRule = FlowRuleManager.getRules().get(1);
        Assert.assertEquals(rule2.getGrade().intValue(), flowRule.getGrade());
        Assert.assertEquals(rule2.getUri(), flowRule.getResource());
        Assert.assertEquals(rule2.getThreshold(), flowRule.getCount(), 0.000001);
        Assert.assertEquals(rule2.getControlBehavior().intValue(), flowRule.getControlBehavior());
        Assert.assertEquals(rule2.getMaxTimeoutMs().intValue(), flowRule.getMaxQueueingTimeMs());
        Assert.assertEquals(rule2, expectedListener.loadedFlowRules.stream().findFirst().get());
        Assert.assertEquals(1, ruleManager.sentinelRules.size());

        // Delete all
        ruleManager.deleteFlowControlRule("Command", Arrays.asList("1", "2"), true);
        Assert.assertEquals(0, FlowRuleManager.getRules().size());
        Assert.assertEquals(0, ruleManager.flowId2Rules.size());
        Assert.assertEquals(0, ruleManager.sentinelRules.size());
        Assert.assertEquals(2, expectedListener.unLoadedFlowRules.size());
    }

    @Test
    public void repeatedAddAndDelete() {
        ExpectedListener expectedListener = new ExpectedListener();
        SentinelRuleManager ruleManager = new SentinelRuleManager(expectedListener);

        for (int i = 0; i < 10; i++) {
            FlowRuleDto rule1 = new FlowRuleDto();
            {
                rule1.setRuleId("1");
                rule1.setUri("/uri/1");
                rule1.setControlBehavior(0);
                rule1.setThreshold(100);
                rule1.setGrade(0);
                rule1.setMaxTimeoutMs(500);
            }
            ruleManager.addFlowControlRule("Command", rule1, true);

            Assert.assertEquals(1, FlowRuleManager.getRules().size());
            FlowRule flowRule = FlowRuleManager.getRules().get(0);
            Assert.assertEquals(rule1.getGrade().intValue(), flowRule.getGrade());
            Assert.assertEquals(rule1.getUri(), flowRule.getResource());
            Assert.assertEquals(rule1.getThreshold(), flowRule.getCount(), 0.000001);
            Assert.assertEquals(rule1.getControlBehavior().intValue(), flowRule.getControlBehavior());
            Assert.assertEquals(rule1.getMaxTimeoutMs().intValue(), flowRule.getMaxQueueingTimeMs());
            Assert.assertEquals(rule1, expectedListener.loadedFlowRules.stream().findFirst().get());

            FlowRuleDto rule2 = new FlowRuleDto();
            {
                rule2.setRuleId("2");
                rule2.setUri("/uri/1");
                rule2.setControlBehavior(0);
                rule2.setThreshold(200);
                rule2.setGrade(0);
                rule2.setMaxTimeoutMs(500);
            }
            ruleManager.addFlowControlRule("Command", rule2, true);
            Assert.assertEquals(2, FlowRuleManager.getRules().size());
            flowRule = FlowRuleManager.getRules().get(0);
            Assert.assertEquals(rule1.getGrade().intValue(), flowRule.getGrade());
            Assert.assertEquals(rule1.getUri(), flowRule.getResource());
            Assert.assertEquals(rule1.getThreshold(), flowRule.getCount(), 0.000001);
            Assert.assertEquals(rule1.getControlBehavior().intValue(), flowRule.getControlBehavior());
            Assert.assertEquals(rule1.getMaxTimeoutMs().intValue(), flowRule.getMaxQueueingTimeMs());
            flowRule = FlowRuleManager.getRules().get(1);
            Assert.assertEquals(rule2.getGrade().intValue(), flowRule.getGrade());
            Assert.assertEquals(rule2.getUri(), flowRule.getResource());
            Assert.assertEquals(rule2.getThreshold(), flowRule.getCount(), 0.000001);
            Assert.assertEquals(rule2.getControlBehavior().intValue(), flowRule.getControlBehavior());
            Assert.assertEquals(rule2.getMaxTimeoutMs().intValue(), flowRule.getMaxQueueingTimeMs());
            Assert.assertEquals(rule2, expectedListener.loadedFlowRules.stream().findFirst().get());
            Assert.assertEquals(1, ruleManager.sentinelRules.size());

            // Delete all
            ruleManager.deleteFlowControlRule("Command", Arrays.asList("1", "2"), true);
            Assert.assertEquals(0, FlowRuleManager.getRules().size());
            Assert.assertEquals(0, ruleManager.flowId2Rules.size());
            Assert.assertEquals(0, ruleManager.sentinelRules.size());
            Assert.assertEquals(2, expectedListener.unLoadedFlowRules.size());
        }
    }

    @Test
    public void testConsistencyCheck() {
        ExpectedListener expectedListener = new ExpectedListener();
        SentinelRuleManager ruleManager = new SentinelRuleManager(expectedListener);
        FlowRuleDto rule1 = new FlowRuleDto();
        {
            rule1.setRuleId("1");
            rule1.setUri("/uri/1");
            rule1.setControlBehavior(0);
            rule1.setThreshold(100);
            rule1.setGrade(0);
            rule1.setMaxTimeoutMs(500);
        }
        FlowRuleDto rule2 = new FlowRuleDto();
        {
            rule2.setRuleId("2");
            rule2.setUri("/uri/2");
            rule2.setControlBehavior(0);
            rule2.setThreshold(100);
            rule2.setGrade(0);
            rule2.setMaxTimeoutMs(500);
        }
        FlowRuleDto rule3 = new FlowRuleDto();
        {
            rule3.setRuleId("3");
            rule3.setUri("/uri/3");
            rule3.setControlBehavior(0);
            rule3.setThreshold(100);
            rule3.setGrade(0);
            rule3.setMaxTimeoutMs(500);
        }
        ruleManager.addFlowControlRule("Command", rule1, true);
        ruleManager.addFlowControlRule("Command", rule2, true);
        ruleManager.addFlowControlRule("Command", rule3, true);

        Assert.assertEquals(3, FlowRuleManager.getRules().size());

        Map<String, FlowRuleDto> config = new HashMap<>();

        FlowRuleDto rule4 = new FlowRuleDto();
        {
            rule4.setRuleId("4");
            rule4.setUri("/uri/4");
            rule4.setControlBehavior(0);
            rule4.setThreshold(200);
            rule4.setGrade(0);
            rule4.setMaxTimeoutMs(500);
        }
        FlowRuleDto rule1Changed = new FlowRuleDto();
        rule1Changed.setRuleId("1");
        rule1Changed.setUri("/uri/1changed");
        rule1Changed.setControlBehavior(0);
        rule1Changed.setThreshold(100);
        rule1Changed.setGrade(0);
        rule1Changed.setMaxTimeoutMs(500);

        config.put("1", rule1Changed);
        config.put("2", rule2);
        config.put("4", rule4);
        ruleManager.checkFlowRule(config);

        Assert.assertEquals(3, FlowRuleManager.getRules().size());

        FlowRule flowRule = FlowRuleManager.getRules().get(0);
        Assert.assertEquals(rule2.getGrade().intValue(), flowRule.getGrade());
        Assert.assertEquals(rule2.getUri(), flowRule.getResource());
        Assert.assertEquals(rule2.getThreshold(), flowRule.getCount(), 0.000001);
        Assert.assertEquals(rule2.getControlBehavior().intValue(), flowRule.getControlBehavior());
        Assert.assertEquals(rule2.getMaxTimeoutMs().intValue(), flowRule.getMaxQueueingTimeMs());

        flowRule = FlowRuleManager.getRules().get(1);
        Assert.assertEquals(rule4.getGrade().intValue(), flowRule.getGrade());
        Assert.assertEquals(rule4.getUri(), flowRule.getResource());
        Assert.assertEquals(rule4.getThreshold(), flowRule.getCount(), 0.000001);
        Assert.assertEquals(rule4.getControlBehavior().intValue(), flowRule.getControlBehavior());
        Assert.assertEquals(rule4.getMaxTimeoutMs().intValue(), flowRule.getMaxQueueingTimeMs());

        flowRule = FlowRuleManager.getRules().get(2);
        Assert.assertEquals(rule1Changed.getGrade().intValue(), flowRule.getGrade());
        Assert.assertEquals(rule1Changed.getUri(), flowRule.getResource());
        Assert.assertEquals(rule1Changed.getThreshold(), flowRule.getCount(), 0.000001);
        Assert.assertEquals(rule1Changed.getControlBehavior().intValue(), flowRule.getControlBehavior());
        Assert.assertEquals(rule1Changed.getMaxTimeoutMs().intValue(), flowRule.getMaxQueueingTimeMs());

        SentinelRuleManager.CompositeRule cr = ruleManager.matches(rule1Changed.getUri());
        Assert.assertEquals(1, cr.getFlowRules().size());
        SentinelRuleManager.CompositeRule cr2 = ruleManager.matches(rule2.getUri());
        Assert.assertEquals(1, cr2.getFlowRules().size());
        SentinelRuleManager.CompositeRule cr4 = ruleManager.matches(rule4.getUri());
        Assert.assertEquals(1, cr4.getFlowRules().size());
        SentinelRuleManager.CompositeRule cr3 = ruleManager.matches(rule3.getUri());
        Assert.assertNull(cr3);
    }
}
