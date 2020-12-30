package com.sbss.bithon.webserver.page;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 12:17 上午
 */
@Controller
public class HomeController {

    @GetMapping("/ui/home")
    public String home() {
        return "home/index";
    }
}
