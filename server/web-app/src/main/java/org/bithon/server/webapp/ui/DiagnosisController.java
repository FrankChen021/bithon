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

import lombok.extern.slf4j.Slf4j;
import org.bithon.server.webapp.WebAppModuleEnabler;
import org.bithon.server.webapp.services.ServiceDiscovery;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author Frank Chen
 * @date 1/3/23 10:13 pm
 */
@Slf4j
@Controller
@Conditional(value = WebAppModuleEnabler.class)
public class DiagnosisController {

    private final ServiceDiscovery serviceDiscovery;

    public DiagnosisController(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    @GetMapping("/web/diagnosis/diagnosis")
    public String traceList(Model model) {
        model.addAttribute("apiHost", serviceDiscovery.getApiHost());
        return "diagnosis/diagnosis";
    }
}
