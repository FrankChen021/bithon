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

    @GetMapping("/ui/app/{appName}/{module}")
    public String webServerPage(@PathVariable("appName") String appName,
                                @PathVariable("module") String module,
                                Model model) {
        model.addAttribute("appName", appName);
        return "app/" + module;
    }

    @GetMapping("/ui/app/{appName}/trace")
    public String traceHomePage(@PathVariable("appName") String appName) {
        return "app/trace";
    }

}
