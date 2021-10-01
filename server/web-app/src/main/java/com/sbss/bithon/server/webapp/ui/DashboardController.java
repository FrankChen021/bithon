/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.server.webapp.ui;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author frank.chen021@outlook.com
 * @date 2021-10-01
 */
@RestController
public class DashboardController {

    @GetMapping("/ui/api/dashboard/{boardName}")
    public void getBoardConfig(@PathVariable("boardName") String boardName, HttpServletResponse response) {
        response.setContentType("application/json");

        // for now, it loads config from static file
        // in future it can be changed to load from external storage
        try (InputStream is = DashboardController.class.getClassLoader().getResourceAsStream("dashboard/" + boardName + ".json")) {
            if ( is == null ) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
                return;
            }

            byte[] buf = new byte[1024];
            int len = 0;
            do {
                len = is.read(buf);
                response.getOutputStream().write(buf, 0, len);
            } while (len == 1024);
            response.setStatus(HttpStatus.OK.value());
        } catch (Exception e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            try {
                response.getWriter().write(e.toString());
            } catch (IOException ignored) {
            }
        }
    }
}
