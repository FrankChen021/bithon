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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This is a temporary solution that reads configuration from file
 * in future it should read from a remote storage such as DB
 *
 * @author frank.chen021@outlook.com
 * @date 2021-10-01
 */
@Slf4j
@RestController
public class DashboardController {

    @Getter
    @AllArgsConstructor
    public static class DisplayableText {
        private final String value;
        private final String text;
    }

    private final Map<String, LoadedDashboard> dashboardConfigs;
    private final List<DisplayableText> dashboardNames;

    static class DashboardLoader {
        private final ObjectMapper om;
        private final ResourceLoader resourceLoader;

        DashboardLoader(ObjectMapper om, ResourceLoader resourceLoader) {
            this.om = om;
            this.resourceLoader = resourceLoader;
        }

        public Map<String, LoadedDashboard> loadDashboard() {
            Map<String, LoadedDashboard> dashboardConfigs = new HashMap<>();
            try {
                Resource[] resources = ResourcePatternUtils.getResourcePatternResolver(resourceLoader)
                                                           .getResources("classpath:/dashboard/*.json");
                for (Resource resource : resources) {
                    try {
                        LoadedDashboard board = LoadedDashboard.load(om,
                                                                     resource.getFilename(),
                                                                     resource.getInputStream());
                        dashboardConfigs.put(board.id, board);
                    } catch (IOException e) {
                        log.error("Error loading {}: {}}", resource.getFilename(), e.getMessage());
                    }
                }
            } catch (IOException e) {
                log.error("Error when loading dashboard resources", e);
            }
            return dashboardConfigs;
        }
    }

    @Data
    static class Dashboard {
        private String title;
    }

    @Data
    @AllArgsConstructor
    static class LoadedDashboard {
        private String id;
        private String title;
        private byte[] stream;

        public static LoadedDashboard load(ObjectMapper om, String name, InputStream is) throws IOException {
            byte[] stream = toByteArray(is);
            Dashboard board = om.readValue(stream, Dashboard.class);

            return new LoadedDashboard(name.substring(0, name.length() - ".json".length()),
                                       board.getTitle(),
                                       stream);
        }

        private static byte[] toByteArray(InputStream is) throws IOException {
            try (ByteArrayOutputStream bs = new ByteArrayOutputStream()) {
                byte[] buf = new byte[1024];
                int len;
                do {
                    len = is.read(buf);
                    bs.write(buf, 0, len);
                } while (len == 1024);
                return bs.toByteArray();
            }
        }
    }

    public DashboardController(ObjectMapper om, ResourceLoader resourceLoader) {
        this.dashboardConfigs = new DashboardLoader(om, resourceLoader).loadDashboard();
        this.dashboardNames = this.dashboardConfigs.values()
                                                   .stream()
                                                   .map(dashboard -> new DisplayableText(dashboard.id, dashboard.title))
                                                   .sorted(Comparator.comparing(o -> o.text))
                                                   .collect(Collectors.toList());
    }

    @GetMapping("/web/api/dashboard/names")
    public List<DisplayableText> getDashBoardConfig() {
        return dashboardNames;
    }

    @GetMapping("/web/api/dashboard/get/{boardName}")
    public void getDashBoardConfig(@PathVariable("boardName") String boardName, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        LoadedDashboard board = dashboardConfigs.get(boardName);
        if (board == null) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }
        response.getOutputStream().write(board.getStream());
        response.setStatus(HttpStatus.OK.value());
    }
}
