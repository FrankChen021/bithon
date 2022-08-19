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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.web.Dashboard;
import org.bithon.server.webapp.WebAppModuleEnabler;
import org.bithon.server.webapp.services.DashboardManager;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
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
@Conditional(value = WebAppModuleEnabler.class)
public class DashboardController {

    private final DashboardManager dashboardManager;
    private final ObjectMapper objectMapper;

    @Getter
    @AllArgsConstructor
    public static class DisplayableText {
        private final String value;

        // display text
        private final String text;
    }

    @Data
    public static class DashboardTitle {
        private String title;
    }

    private List<DisplayableText> dashboardList;

    public DashboardController(DashboardManager dashboardManager,
                               ObjectMapper objectMapper) {
        this.dashboardManager = dashboardManager;
        this.dashboardManager.addListener(this::updateDashboardList);
        this.objectMapper = objectMapper;
    }

    private void updateDashboardList() {
        dashboardList = this.dashboardManager.getDashboards()
                                             .stream()
                                             .map(dashboard -> {
                                                 String title = "";
                                                 try {
                                                     title = objectMapper.readValue(dashboard.getPayload(), DashboardTitle.class).title;
                                                 } catch (JsonProcessingException ignored) {
                                                 }
                                                 return new DisplayableText(dashboard.getName(), title);
                                             })
                                             .sorted(Comparator.comparing(o -> o.text))
                                             .collect(Collectors.toList());
    }

    @GetMapping("/web/api/dashboard/names")
    public List<DisplayableText> getDashboardNames() {
        if (dashboardList == null) {
            // no need to sync because it's acceptable
            updateDashboardList();
        }
        return dashboardList;
    }

    @GetMapping("/web/api/dashboard/all")
    public void getAllDashboards(HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        try (OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream())) {
            writer.write('[');
            {
                List<Dashboard> dashboards = this.dashboardManager.getDashboards();

                // sort by name
                dashboards.sort(Comparator.comparing(Dashboard::getName));

                for (int i = 0; i < dashboards.size(); i++) {

                    writer.write("\"");
                    writer.write(dashboards.get(i).getName());
                    writer.write("\"");
                    writer.write(":");
                    writer.write(dashboards.get(i).getPayload());

                    if (i < dashboards.size() - 1) {
                        writer.write(',');
                    }
                }
            }
            writer.write(']');

            response.setStatus(HttpStatus.OK.value());
        }
    }

    @GetMapping("/web/api/dashboard/get/{boardName}")
    public void getDashboard(@PathVariable("boardName") String boardName, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        Dashboard board = this.dashboardManager.getDashboard(boardName);
        if (board == null) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }
        try (OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream())) {
            writer.write(board.getPayload());
            response.setStatus(HttpStatus.OK.value());
        }
    }

    @PostMapping("/web/api/dashboard/update/{boardName}")
    public void updateDashboard(@PathVariable("boardName") String name, HttpServletRequest request, HttpServletResponse response) throws IOException {

        // check if it's well-formed
        JsonNode dashboard = objectMapper.readTree(request.getInputStream());

        JsonNode titleNode = dashboard.get("title");
        if (titleNode == null || StringUtils.isBlank(titleNode.asText())) {
            response.getWriter().println("title is missing.");
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return;
        }

        this.dashboardManager.update(name, objectMapper.writeValueAsString(dashboard));
    }
}
