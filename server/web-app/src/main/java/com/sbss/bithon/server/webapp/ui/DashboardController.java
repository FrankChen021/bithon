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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import sun.misc.Launcher;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * This is a temporary solution that reads configuration from file
 * in future it should read from a remote storage such as DB
 *
 * @author frank.chen021@outlook.com
 * @date 2021-10-01
 */
@RestController
public class DashboardController {

    @Getter
    @AllArgsConstructor
    public class DisplayableText {
        private final String value;
        private final String text;
    }

    private Map<String, LoadedDashboard> dashboardConfigs = new HashMap<>();
    private List<DisplayableText> dashboardNames;
    private final ObjectMapper om;

    public void loadDashboard() throws IOException {
        final String path = "dashboard";
        final File jarFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());

        if (jarFile.isFile()) {
            final JarFile jar = new JarFile(jarFile);
            final Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                final String name = jarEntry.getName();
                if (name.startsWith(path + "/") && name.endsWith(".json")) {
                    LoadedDashboard board = LoadedDashboard.load(om,
                                                                 name,
                                                                 jar.getInputStream(jarEntry));
                    dashboardConfigs.put(board.id, board);
                }
            }
            jar.close();
        } else { // Run with IDE
            final URL url = Launcher.class.getResource("/" + path);
            if (url != null) {
                try {
                    final File dir = new File(url.toURI());
                    for (File file : dir.listFiles()) {
                        if (!file.getName().endsWith(".json")) {
                            continue;
                        }

                        try (InputStream is = new FileInputStream(file)) {
                            LoadedDashboard board = LoadedDashboard.load(om,
                                                                         file.getName(),
                                                                         is);
                            dashboardConfigs.put(board.id, board);
                        }
                    }
                } catch (URISyntaxException ex) {
                    // never happens
                }
            }
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

    public DashboardController(ObjectMapper om) {
        this.om = om;
        try {
            loadDashboard();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
