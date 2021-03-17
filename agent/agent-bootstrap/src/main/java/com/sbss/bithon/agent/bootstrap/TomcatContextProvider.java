package com.sbss.bithon.agent.bootstrap;

import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.io.File.separator;

/**
 * @author frankchen
 */
public class TomcatContextProvider {

    private static final Logger log = LoggerFactory.getLogger(TomcatContextProvider.class);

    private static File findBaseLibPath(String catalinaHome) {
        File result = new File(catalinaHome + separator + "lib");
        log.info(String.format("Base libs path: %s", result.getAbsolutePath()));
        return result;
    }

    private static List<File> findAppLibPaths(String catalinaHome) {
        List<File> result = new ArrayList<>();
        File webApps = new File(catalinaHome + separator + "webapps");
        File[] appPaths = webApps.listFiles(File::isDirectory);
        if (null != appPaths) {
            result.addAll(Arrays.stream(appPaths)
                                .map(p -> new File(p.getAbsolutePath() + separator + "WEB-INF" + separator + "lib"))
                                .filter(File::isDirectory)
                                .collect(Collectors.toList())
            );
        }
        return result;
    }

    public static List<File> findLibs(String catalinaHome) throws IOException {
        List<File> result = new ArrayList<>();
        File[] baseJarLibs = findBaseLibPath(catalinaHome).listFiles(file -> file.getName()
                                                                                 .toLowerCase()
                                                                                 .endsWith(Constant.Suffix.JAR.value()));
        if (null != baseJarLibs) {
            result.addAll(Arrays.stream(baseJarLibs).collect(Collectors.toList()));
        }
        for (File appPath : findAppLibPaths(catalinaHome)) {
            log.info(String.format("App libs path: %s", appPath.getAbsolutePath()));
            File[] libs = appPath.listFiles(file -> file.getName().toLowerCase().endsWith(Constant.Suffix.JAR.value()));
            if (null != libs) {
                result.addAll(Arrays.stream(libs).collect(Collectors.toList()));
            }
        }
        return result;
    }
}
