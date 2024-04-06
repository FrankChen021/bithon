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

package org.bithon.server.webapp.ui;

import org.bithon.server.webapp.WebAppModuleEnabler;
import org.bithon.server.webapp.services.ServiceDiscovery;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/29
 */
@Controller
@Conditional(value = WebAppModuleEnabler.class)
public class AlertController {

    private final ServiceDiscovery serviceDiscovery;

    public AlertController(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    @GetMapping("/web/alerting/alert/create")
    public String createAlert(@RequestParam(value = "appName", required = false) String appName,
                              Model m) {
        m.addAttribute("appName", appName);
        m.addAttribute("apiHost", serviceDiscovery.getApiHost());
        m.addAttribute("model", "alert");
        return "alerting/alert/create";
    }

    @GetMapping("/web/alerting/alert/list")
    public String getAlertList(@RequestParam(value = "appName", required = false) String appName,
                               @RequestParam(value = "env", required = false) String env,
                               Model m) {
        m.addAttribute("appName", appName);
        m.addAttribute("env", env);
        m.addAttribute("apiHost", serviceDiscovery.getApiHost());
        m.addAttribute("model", "alert");
        return "alerting/alert/list";
    }

    @GetMapping("/web/alerting/alert/detail")
    public String showAlertDetail(@RequestParam(value = "id") String alertId,
                                  @RequestParam(value = "returnUrl", required = false) String returnUrl,
                                  Model m) {
        m.addAttribute("alertId", alertId);
        m.addAttribute("returnUrl", returnUrl);
        m.addAttribute("apiHost", serviceDiscovery.getApiHost());
        m.addAttribute("model", "alert");
        return "alerting/alert/detail";
    }

    @GetMapping("/web/alerting/alert/update")
    public String updateAlert(@RequestParam(value = "id") String alertId, Model m) {
        m.addAttribute("alertId", alertId);
        m.addAttribute("apiHost", serviceDiscovery.getApiHost());
        m.addAttribute("model", "alert");
        return "alerting/alert/update";
    }

    /**
     * The alerting record frame page that consists of the list and the detail below
     */
    @GetMapping("/web/alerting/records")
    public String alertRecordsPage(Model m) {
        m.addAttribute("model", "alert");
        return "alerting/record/records";
    }

    @GetMapping("/web/alerting/record/list")
    public String recordListPage(Model m) {
        m.addAttribute("apiHost", serviceDiscovery.getApiHost());
        m.addAttribute("model", "alert");
        return "alerting/record/list";
    }

    @GetMapping("/web/alerting/record/detail")
    public String showRecordDetail(Model m) {
        m.addAttribute("apiHost", serviceDiscovery.getApiHost());
        m.addAttribute("model", "alert");
        return "alerting/record/detail";
    }

    /**
     * Create notification channel
     */
    @GetMapping("/web/alerting/channel/create")
    public String createChannel(Model m) {
        m.addAttribute("apiHost", serviceDiscovery.getApiHost());
        m.addAttribute("model", "alert");
        return "alerting/channel/create";
    }

    @GetMapping("/web/alerting/channel/list")
    public String listChannel(Model m) {
        m.addAttribute("apiHost", serviceDiscovery.getApiHost());
        m.addAttribute("model", "alert");
        return "alerting/channel/list";
    }
}
