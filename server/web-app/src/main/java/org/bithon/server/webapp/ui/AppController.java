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

import org.bithon.server.webapp.services.ServiceDiscovery;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 12:17 上午
 */
@Controller
public class AppController {

    private final ServiceDiscovery serviceDiscovery;

    public AppController(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    @GetMapping("/web/app/*")
    public String appHomePage(Model model) {
        model.addAttribute("apiHost", serviceDiscovery.getApiHost());
        return "app/index";
    }

    @GetMapping("/web/app/metric/{appName}/{dashboardName}")
    public String webServerPage(@PathVariable("appName") String appName,
                                @PathVariable("dashboardName") String dashboardName,
                                @RequestParam(value = "interval", required = false) String interval,
                                @RequestParam(value = "instance", required = false) String instance,
                                Model model) {
        model.addAttribute("apiHost", serviceDiscovery.getApiHost());
        model.addAttribute("appName", appName);
        model.addAttribute("dashboardName", dashboardName);
        model.addAttribute("interval", interval);
        model.addAttribute("instance", instance);
        return "app/dashboard";
    }

    @GetMapping("/web/app/trace/{appName}")
    public String traceHomePage(@PathVariable("appName") String appName,
                                Model model) {
        model.addAttribute("apiHost", serviceDiscovery.getApiHost());
        model.addAttribute("appName", appName);
        return "app/trace";
    }

    @GetMapping("/web/app/topo/{appName}")
    public String topoHome(@PathVariable("appName") String appName,
                           Model model) {
        model.addAttribute("apiHost", serviceDiscovery.getApiHost());
        model.addAttribute("appName", appName);
        return "app/topo";
    }

    @GetMapping("/web/app/event/{appName}")
    public String eventHome(@PathVariable("appName") String appName, Model model) {
        model.addAttribute("apiHost", serviceDiscovery.getApiHost());
        model.addAttribute("appName", appName);
        return "app/event";
    }
}
