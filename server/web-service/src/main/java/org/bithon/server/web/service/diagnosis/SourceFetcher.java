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

package org.bithon.server.web.service.diagnosis;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.Optional;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

public final class SourceFetcher {
    private SourceFetcher() {}

    /**
     * Interface for receiving progress notifications during source fetching operations.
     */
    public interface IProgressNotifier {
        /**
         * Called to notify about progress in the source fetching process.
         * @param timestamp The timestamp when this progress event occurred
         * @param message A descriptive message about the current step
         */
        void notifyProgress(LocalDateTime timestamp, String message);

        /**
         * Called when an error occurs during the process.
         * @param message Error message
         * @param throwable Optional throwable that caused the error
         */
        default void notifyError(String message, Throwable throwable) {
            notifyProgress(LocalDateTime.now(), "ERROR: " + message + (throwable != null ? " - " + throwable.getMessage() : ""));
        }
    }

    public static final class Coordinates {
        public final String groupId, artifactId, version;
        public Coordinates(String g, String a, String v) { groupId=g; artifactId=a; version=v; }
        @Override public String toString() { return groupId + ":" + artifactId + ":" + version; }
    }

    /**
     * Fetch source for the given class name, using the given jar file name to locate the artifact.
     * Returns Optional.empty() if anything can't be resolved (kept intentionally simple).
     */
    public static Optional<String> getSourceForClass(String fullyQualifiedClassName, String jarFileName) throws Exception {
        return getSourceForClass(fullyQualifiedClassName, jarFileName, null);
    }

    /**
     * Fetch source for the given class name, using the given jar file name to locate the artifact.
     * Returns Optional.empty() if anything can't be resolved (kept intentionally simple).
     * @param progressNotifier Optional progress notifier to receive updates during the process
     */
    public static Optional<String> getSourceForClass(String fullyQualifiedClassName, String jarFileName, IProgressNotifier progressNotifier) throws Exception {
        if (progressNotifier != null) {
            progressNotifier.notifyProgress(LocalDateTime.now(), "Starting source fetch for class: " + fullyQualifiedClassName);
        }

        if (fullyQualifiedClassName == null || fullyQualifiedClassName.isEmpty()) {
            if (progressNotifier != null) {
                progressNotifier.notifyError("Invalid class name provided", null);
            }
            return Optional.empty();
        }
        if (jarFileName == null || jarFileName.isEmpty()) {
            if (progressNotifier != null) {
                progressNotifier.notifyError("Invalid jar file name provided", null);
            }
            return Optional.empty();
        }

        if (progressNotifier != null) {
            progressNotifier.notifyProgress(LocalDateTime.now(), "Searching for jar file: " + jarFileName);
        }
        Path jarPath = findJarOnClassPath(jarFileName);
        if (jarPath == null || !Files.isReadable(jarPath)) {
            if (progressNotifier != null) {
                progressNotifier.notifyError("Jar file not found or not readable: " + jarFileName, null);
            }
            return Optional.empty();
        }
        if (progressNotifier != null) {
            progressNotifier.notifyProgress(LocalDateTime.now(), "Found jar file at: " + jarPath);
        }

        if (progressNotifier != null) {
            progressNotifier.notifyProgress(LocalDateTime.now(), "Reading Maven coordinates from jar file");
        }
        Optional<Coordinates> gav = readGavFromJar(jarPath);
        if (!gav.isPresent()) {
            if (progressNotifier != null) {
                progressNotifier.notifyProgress(LocalDateTime.now(), "No pom.properties found, attempting to parse coordinates from filename");
            }
            // very naive fallback: artifactId-version.jar → (artifactId, version)
            Coordinates guess = parseCoordsFromFileName(jarFileName);
            if (guess == null) {
                if (progressNotifier != null) {
                    progressNotifier.notifyError("Could not determine Maven coordinates from jar file or filename", null);
                }
                return Optional.empty();
            }
            gav = Optional.of(guess);
        }
        if (progressNotifier != null) {
            progressNotifier.notifyProgress(LocalDateTime.now(), "Resolved Maven coordinates: " + gav.get());
        }

        if (progressNotifier != null) {
            progressNotifier.notifyProgress(LocalDateTime.now(), "Preparing cache directory");
        }
        Path cacheDir = Paths.get(System.getProperty("java.io.tmpdir"), "sf-cache");
        Files.createDirectories(cacheDir);
        
        if (progressNotifier != null) {
            progressNotifier.notifyProgress(LocalDateTime.now(), "Downloading sources jar from Maven Central");
        }
        Path sourcesJar = downloadSourcesJarFromMavenCentral(gav.get(), cacheDir, progressNotifier);

        if (progressNotifier != null) {
            progressNotifier.notifyProgress(LocalDateTime.now(), "Searching for source code in sources jar");
        }
        Optional<String> result = readSourceFromSourcesJar(sourcesJar, fullyQualifiedClassName, progressNotifier);
        
        if (progressNotifier != null) {
            if (result.isPresent()) {
                progressNotifier.notifyProgress(LocalDateTime.now(), "Successfully found source code for " + fullyQualifiedClassName);
            } else {
                progressNotifier.notifyProgress(LocalDateTime.now(), "Source code not found for " + fullyQualifiedClassName);
            }
        }
        
        return result;
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    private static Path findJarOnClassPath(String jarFileName) {
        String cp = System.getProperty("java.class.path", "");
        String sep = System.getProperty("path.separator");
        for (String entry : cp.split(Pattern.quote(sep))) {
            try {
                Path p = Paths.get(entry);
                if (Files.isRegularFile(p) && p.getFileName().toString().equals(jarFileName)) {
                    return p.toAbsolutePath().normalize();
                }
            } catch (Exception ignore) {}
        }
        return null;
    }

    private static Optional<Coordinates> readGavFromJar(Path jarPath) {
        try (JarFile jf = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> en = jf.entries();
            while (en.hasMoreElements()) {
                JarEntry e = en.nextElement();
                if (e.isDirectory()) continue;
                if (e.getName().startsWith("META-INF/maven/") && e.getName().endsWith("/pom.properties")) {
                    Properties p = new Properties();
                    try (InputStream is = jf.getInputStream(e)) { p.load(is); }
                    String g = p.getProperty("groupId");
                    String a = p.getProperty("artifactId");
                    String v = p.getProperty("version");
                    if (g != null && a != null && v != null) return Optional.of(new Coordinates(g, a, v));
                }
            }
        } catch (IOException ignore) {}
        return Optional.empty();
    }

    private static Coordinates parseCoordsFromFileName(String jarFileName) {
        // super naive: split last '-' as version
        String base = jarFileName.endsWith(".jar") ? jarFileName.substring(0, jarFileName.length()-4) : jarFileName;
        int idx = base.lastIndexOf('-');
        if (idx <= 0 || idx == base.length()-1) return null;
        String artifactId = base.substring(0, idx);
        String version = base.substring(idx+1);
        // groupId unknown; Maven Central path will need it, so this only works when pom.properties was present.
        // For simplicity as requested, we assume pom.properties exists; otherwise this will likely fail.
        return new Coordinates("UNKNOWN_GROUP", artifactId, version);
    }

    private static Path downloadSourcesJarFromMavenCentral(Coordinates gav, Path cacheDir, IProgressNotifier progressNotifier) throws IOException {
        String fileName = gav.artifactId + "-" + gav.version + "-sources.jar";
        Path dest = cacheDir.resolve(fileName);
        if (Files.exists(dest) && Files.size(dest) > 0) {
            if (progressNotifier != null) {
                progressNotifier.notifyProgress(LocalDateTime.now(), "Using cached sources jar: " + fileName);
            }
            return dest;
        }

        // Requires a real groupId; for simplicity we assume pom.properties provided it.
        String path = gav.groupId.replace('.', '/') + "/" + gav.artifactId + "/" + gav.version + "/" + fileName;
        URL url = new URL("https://repo1.maven.org/maven2/" + path);
        
        if (progressNotifier != null) {
            progressNotifier.notifyProgress(LocalDateTime.now(), "Downloading from: " + url);
        }

        try (InputStream in = url.openStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            if (progressNotifier != null) {
                progressNotifier.notifyProgress(LocalDateTime.now(), "Successfully downloaded sources jar: " + fileName);
            }
        } catch (IOException e) {
            if (progressNotifier != null) {
                progressNotifier.notifyError("Failed to download sources jar from " + url, e);
            }
            throw e;
        }
        return dest;
    }

    private static Optional<String> readSourceFromSourcesJar(Path sourcesJar, String fqcn, IProgressNotifier progressNotifier) throws IOException {
        String rel = fqcn.replace('.', '/');
        String[] candidates = { rel + ".java", rel + ".kt", rel + ".scala" };

        try (JarFile jf = new JarFile(sourcesJar.toFile())) {
            if (progressNotifier != null) {
                progressNotifier.notifyProgress(LocalDateTime.now(), "Searching for direct path matches: " + String.join(", ", candidates));
            }
            // direct path
            for (String c : candidates) {
                JarEntry e = jf.getJarEntry(c);
                if (e != null) {
                    if (progressNotifier != null) {
                        progressNotifier.notifyProgress(LocalDateTime.now(), "Found source file: " + c);
                    }
                    return Optional.of(readAll(jf.getInputStream(e)));
                }
            }
            // inner class fallback: match by simple name stem
            if (progressNotifier != null) {
                progressNotifier.notifyProgress(LocalDateTime.now(), "No direct match found, searching for inner class patterns");
            }
            String simple = fqcn.substring(fqcn.lastIndexOf('.') + 1);
            String stem = "/" + simple + ".";
            Enumeration<JarEntry> en = jf.entries();
            while (en.hasMoreElements()) {
                JarEntry e = en.nextElement();
                if (e.isDirectory()) continue;
                String n = e.getName();
                if ((n.endsWith(".java") || n.endsWith(".kt") || n.endsWith(".scala")) && n.contains(stem)) {
                    if (progressNotifier != null) {
                        progressNotifier.notifyProgress(LocalDateTime.now(), "Found potential inner class source file: " + n);
                    }
                    return Optional.of(readAll(jf.getInputStream(e)));
                }
            }
        }
        return Optional.empty();
    }

    private static String readAll(InputStream is) throws IOException {
        try (InputStream in = is) { return new String(in.readAllBytes(), StandardCharsets.UTF_8); }
    }

    /**
     * Console implementation of IProgressNotifier that prints progress messages to System.out.
     */
    public static class ConsoleProgressNotifier implements IProgressNotifier {
        private final String prefix;
        private final DateTimeFormatter timeFormatter;
        
        public ConsoleProgressNotifier() {
            this("[SourceFetcher]");
        }
        
        public ConsoleProgressNotifier(String prefix) {
            this.prefix = prefix;
            this.timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        }
        
        @Override
        public void notifyProgress(LocalDateTime timestamp, String message) {
            System.out.println("[" + timestamp.format(timeFormatter) + "] " + prefix + " " + message);
        }
        
        @Override
        public void notifyError(String message, Throwable throwable) {
            LocalDateTime now = LocalDateTime.now();
            System.err.println("[" + now.format(timeFormatter) + "] " + prefix + " ERROR: " + message);
            if (throwable != null) {
                System.err.println("[" + now.format(timeFormatter) + "] " + prefix + " Exception: " + throwable.getMessage());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        ConsoleProgressNotifier notifier = new ConsoleProgressNotifier();
        
        System.out.println("=== Starting Source Fetch Demo ===");
        Optional<String> src = getSourceForClass("feign.form.spring.SpringSingleMultipartFileWriter", "feign-form-spring-3.8.0.jar", notifier);
        
        System.out.println("\n=== Result ===");
        if (src.isPresent()) {
            System.out.println("Source code found (" + src.get().length() + " characters):");
            System.out.println("--- Source Code ---");
            System.out.println(src.get());
        } else {
            System.out.println("Source code not found");
        }
    }
}
