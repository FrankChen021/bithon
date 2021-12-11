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

public class SyncTest {
/*
    @Test
    public void sync() {
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

        JSONObject originalObj = JSON.parseObject(String.format("{\"flowRules\" : [ %s ]}", JSON.toJSONString(rule)));
        AgentSettingManager.onNotification(originalObj);

        // update synced object
        FlowRuleDto updateRule = new FlowRuleDto();
        {
            updateRule.setRuleId("1");
            updateRule.setUri("/uri/1");
            updateRule.setControlBehavior(0);
            updateRule.setThreshold(1000);
            updateRule.setGrade(0);
            updateRule.setMaxTimeoutMs(500);
        }
        JSONObject updatedObj = JSON.parseObject(String.format("{\"flowRules\" : [ %s ]}", JSON.toJSONString(updateRule)));
        AgentSettingManager.onNotification(updatedObj);
        Assert.assertEquals(1, ruleManager.getFlowRules().size());
        Assert.assertEquals(1, FlowRuleManager.getRules().size());
        flowRule = FlowRuleManager.getRules().get(0);
        Assert.assertEquals(updateRule.getGrade().intValue(), flowRule.getGrade());
        Assert.assertEquals(updateRule.getUri(), flowRule.getResource());
        Assert.assertEquals(updateRule.getThreshold(), flowRule.getCount(), 0.000001);
        Assert.assertEquals(updateRule.getControlBehavior().intValue(), flowRule.getControlBehavior());
        Assert.assertEquals(updateRule.getMaxTimeoutMs().intValue(), flowRule.getMaxQueueingTimeMs());
        Assert.assertEquals(updateRule, expectedListener.loadedFlowRules.stream().findFirst().get());

        JSONObject emptyObj = JSON.parseObject("{\"flowRules\" : [ ]}");
        AgentSettingManager.onNotification(emptyObj);
        Assert.assertEquals(0, ruleManager.getFlowRules().size());
        Assert.assertEquals(0, FlowRuleManager.getRules().size());
    }

    @Test
    public void testSyncDegradeRules() {
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
        // sync the same object
        //
        JSONObject originalObj = JSON.parseObject(String.format("{\"degradeRules\" : [ %s ]}", JSON.toJSONString(rule)));
        AgentSettingManager.onNotification(originalObj);
        Assert.assertEquals(1, ruleManager.getDegradeRules().size());
        Assert.assertEquals(1, DegradeRuleManager.getRules().size());

        //
        // sync updated object
        //
        DegradeRuleDto updated = new DegradeRuleDto();
        {
            updated.setRuleId("1");
            updated.setUri("/uri/1");
            updated.setThreshold(0.5);
            updated.setGrade(0);
            updated.setMaxResponseTime(500);
            updated.setTimeWindow(100);
            updated.setStatIntervalMs(5555);
        }
        DegradeRuleDto added = new DegradeRuleDto();
        {
            added.setRuleId("2");
            added.setUri("/uri/2");
            added.setThreshold(0.5);
            added.setGrade(0);
            added.setMaxResponseTime(500);
            added.setTimeWindow(100);
            added.setStatIntervalMs(5555);
        }
        JSONObject updatedObj = JSON.parseObject(String.format("{\"degradeRules\" : [ %s, %s ]}", JSON.toJSONString(updated), JSON.toJSONString(added)));
        AgentSettingManager.onNotification(updatedObj);
        Assert.assertEquals(2, ruleManager.getDegradeRules().size());
        Assert.assertEquals(2, DegradeRuleManager.getRules().size());
        degradeRule = DegradeRuleManager.getRules().get(1);
        Assert.assertEquals(updated.getGrade().intValue(), degradeRule.getGrade());
        Assert.assertEquals(updated.getUri(), degradeRule.getResource());
        Assert.assertEquals(updated.getMaxResponseTime().intValue(), (int) degradeRule.getCount());
        Assert.assertEquals(updated.getThreshold(), degradeRule.getSlowRatioThreshold(), 0.000001);
        Assert.assertEquals(updated.getTimeWindow().intValue(), degradeRule.getTimeWindow());
        Assert.assertEquals(updated.getStatIntervalMs().intValue(), degradeRule.getStatIntervalMs());

        degradeRule = DegradeRuleManager.getRules().get(0);
        Assert.assertEquals(added.getGrade().intValue(), degradeRule.getGrade());
        Assert.assertEquals(added.getUri(), degradeRule.getResource());
        Assert.assertEquals(added.getMaxResponseTime().intValue(), (int) degradeRule.getCount());
        Assert.assertEquals(added.getThreshold(), degradeRule.getSlowRatioThreshold(), 0.000001);
        Assert.assertEquals(added.getTimeWindow().intValue(), degradeRule.getTimeWindow());
        Assert.assertEquals(added.getStatIntervalMs().intValue(), degradeRule.getStatIntervalMs());

        //
        // sync one object
        //
        JSONObject oneObj = JSON.parseObject(String.format("{\"degradeRules\" : [ %s ]}", JSON.toJSONString(added)));
        AgentSettingManager.onNotification(oneObj);
        Assert.assertEquals(1, ruleManager.getDegradeRules().size());
        Assert.assertEquals(1, DegradeRuleManager.getRules().size());
        degradeRule = DegradeRuleManager.getRules().get(0);
        Assert.assertEquals(added.getGrade().intValue(), degradeRule.getGrade());
        Assert.assertEquals(added.getUri(), degradeRule.getResource());
        Assert.assertEquals(added.getMaxResponseTime().intValue(), (int) degradeRule.getCount());
        Assert.assertEquals(added.getThreshold(), degradeRule.getSlowRatioThreshold(), 0.000001);
        Assert.assertEquals(added.getTimeWindow().intValue(), degradeRule.getTimeWindow());
        Assert.assertEquals(added.getStatIntervalMs().intValue(), degradeRule.getStatIntervalMs());

        //
        // sync empty object
        //
        JSONObject empty = JSON.parseObject(String.format("{\"degradeRules\" : []}"));
        AgentSettingManager.onNotification(empty);
        Assert.assertEquals(0, ruleManager.getDegradeRules().size());
        Assert.assertEquals(0, DegradeRuleManager.getRules().size());
    }
 */
}
