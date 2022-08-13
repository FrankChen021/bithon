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

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 8:39 下午
 */
@Controller
@Conditional(value = WebAppModuleEnabler.class)
public class TraceController {

    private final ServiceDiscovery serviceDiscovery;

    public TraceController(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    @GetMapping("/web/trace/list")
    public String traceList(Model model) {
        model.addAttribute("apiHost", serviceDiscovery.getApiHost());
        return "app/trace";
    }

    @GetMapping("/web/trace/detail")
    public String traceHome(Model model) {
        model.addAttribute("apiHost", serviceDiscovery.getApiHost());
        return "trace/detail";
    }

    @GetMapping("/web/trace/search")
    public String search(Model model) {
        model.addAttribute("apiHost", serviceDiscovery.getApiHost());
        return "trace/search";
    }

    @GetMapping("/web/trace/profiling")
    public String profiling(Model model) {
        model.addAttribute("apiHost", serviceDiscovery.getApiHost());
        return "trace/profiling";
    }
}
