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

import org.bithon.component.commons.Experimental;
import org.bithon.server.storage.web.Dashboard;
import org.bithon.server.webapp.WebAppModuleEnabler;
import org.bithon.server.webapp.services.DashboardManager;
import org.bithon.server.webapp.services.ServiceDiscovery;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletResponse;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/5/17 8:39 下午
 */
@Experimental
@Controller
@Conditional(value = WebAppModuleEnabler.class)
public class LogController {

    private final ServiceDiscovery serviceDiscovery;
    private final DashboardManager dashboardManager;

    public LogController(ServiceDiscovery serviceDiscovery, DashboardManager dashboardManager) {
        this.serviceDiscovery = serviceDiscovery;
        this.dashboardManager = dashboardManager;
    }

    @GetMapping("/web/log/{store}")
    public String log(@PathVariable("store") String store, Model model, HttpServletResponse servletResponse) {
        Dashboard dashboard = dashboardManager.getDashboard(store);
        if (dashboard == null) {
            servletResponse.setStatus(HttpStatus.NOT_FOUND.value());
            return null;
        }

        model.addAttribute("apiHost", serviceDiscovery.getApiHost());
        model.addAttribute("store", store);
        model.addAttribute("storeDisplayName", dashboard.getMetadata() == null ? store : dashboard.getMetadata().getTitle());
        return "log/detail";
    }
}
