package com.sbss.bithon.server.webapp.ui;

import com.sbss.bithon.server.webapp.services.ServiceDiscovery;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 12:17 上午
 */
@Controller
public class HomeController {

    private final ServiceDiscovery serviceDiscovery;

    public HomeController(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    @GetMapping("/ui/home")
    public String home(Model model) {
        model.addAttribute("apiHost", serviceDiscovery.getApiHost());
        return "home/index";
    }
}
