package com.sbss.bithon.webserver.page;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 12:17 上午
 */
@Controller
public class AppController {

    @GetMapping("/ui/app/{appName}")
    public String appHomePage(@PathVariable("appName") String appName) {
        return "app/index";
    }

    @GetMapping("/ui/app/metric/{appName}/{metricName}")
    public String webServerPage(@PathVariable("appName") String appName,
                                @PathVariable("metricName") String metricName,
                                Model model) {
        model.addAttribute("appName", appName);
        model.addAttribute("metricName", metricName);
        return "app/metric-template";
    }

    @GetMapping("/ui/app/trace/{appName}")
    public String traceHomePage(@PathVariable("appName") String appName) {
        return "app/trace";
    }

}
