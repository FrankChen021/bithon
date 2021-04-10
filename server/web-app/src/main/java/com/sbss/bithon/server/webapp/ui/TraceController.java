package com.sbss.bithon.server.webapp.ui;

import com.sbss.bithon.server.webapp.services.ServiceDiscovery;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 8:39 下午
 */
@Controller
public class TraceController {

    private final ServiceDiscovery serviceDiscovery;

    public TraceController(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    @GetMapping("/ui/trace/detail/{traceId}")
    public String traceHome(@PathVariable("traceId") String traceId,
                            Model model) {
        model.addAttribute("apiHost", serviceDiscovery.getApiHost());
        model.addAttribute("traceId", traceId);
        return "trace/detail";
    }
}
