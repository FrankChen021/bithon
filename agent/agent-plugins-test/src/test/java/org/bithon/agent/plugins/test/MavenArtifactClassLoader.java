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

package org.bithon.agent.plugins.test;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class for creating URLClassLoader instances from Maven artifacts.
 * This class handles Maven artifact resolution and downloads JAR files to create
 * custom class loaders for loading classes from specified Maven dependencies.
 *
 * @author frankchen
 */
public class MavenArtifactClassLoader {

    private static final Logger log = LoggerFactory.getLogger(MavenArtifactClassLoader.class);

    private MavenArtifactClassLoader() {
    }

    /**
     * Creates a URLClassLoader from the specified Maven artifacts.
     * This method resolves Maven artifacts, downloads them if necessary, and creates a class loader
     * that can load classes from these artifacts.
     *
     * @param artifacts List of Maven artifacts to load classes from
     * @return URLClassLoader configured with the resolved artifact JAR files
     * @throws RuntimeException if artifact resolution or class loader creation fails
     */
    public static URLClassLoader create(MavenArtifact... artifacts) {
        if (artifacts == null || artifacts.length == 0) {
            throw new IllegalArgumentException("Artifacts list cannot be null or empty");
        }

        String list = Stream.of(artifacts)
                            .map(MavenArtifact::getCoordinates)
                            .collect(Collectors.joining(", "));
        log.info("Creating class loader from {} Maven artifacts: {}",
                 artifacts.length,
                 list);

        try {
            // Create Maven repository system
            RepositorySystem repositorySystem = createRepositorySystem();
            RepositorySystemSession session = createRepositorySystemSession(repositorySystem);

            // Create remote repositories (Maven Central)
            List<RemoteRepository> repositories = Collections.singletonList(
                new RemoteRepository.Builder("central", "default", "https://repo1.maven.org/maven2/").build()
            );

            // Resolve artifacts
            List<URL> artifactUrls = new ArrayList<>();
            for (MavenArtifact mavenArtifact : artifacts) {
                log.info("Resolving artifact: {}", mavenArtifact.getCoordinates());

                Artifact artifact = new DefaultArtifact(mavenArtifact.getCoordinates());

                ArtifactRequest artifactRequest = new ArtifactRequest();
                artifactRequest.setArtifact(artifact);
                artifactRequest.setRepositories(repositories);

                ArtifactResult artifactResult = repositorySystem.resolveArtifact(session, artifactRequest);
                File artifactFile = artifactResult.getArtifact().getFile();

                if (artifactFile != null && artifactFile.exists()) {
                    URL artifactUrl = artifactFile.toURI().toURL();
                    artifactUrls.add(artifactUrl);
                    log.info("Added artifact to classpath: {} -> {}",
                             artifactResult.getArtifact().toString(),
                             artifactFile.getAbsolutePath());
                } else {
                    log.warn("Artifact file not found for: {}", artifactResult.getArtifact());
                }
            }

            if (artifactUrls.isEmpty()) {
                throw new RuntimeException("No valid artifact files found for the specified artifacts");
            }

            // Create URLClassLoader with the resolved artifacts
            URL[] urlArray = artifactUrls.toArray(new URL[0]);
            return new URLClassLoader(urlArray, MavenArtifactClassLoader.class.getClassLoader());
        } catch (ArtifactResolutionException e) {
            throw new RuntimeException("Failed to load classes from Maven artifacts:" + e.getMessage());
        } catch (Exception e) {
            log.error(StringUtils.format("Failed to load classes from Maven artifacts: %s", list), e);
            throw new RuntimeException("Failed to load classes from Maven artifacts", e);
        }
    }

    /**
     * Convenience method to create a URLClassLoader from a single Maven artifact.
     *
     * @param groupId    Maven group ID
     * @param artifactId Maven artifact ID
     * @param version    Maven version
     * @return URLClassLoader configured with the resolved artifact JAR file
     */
    public static URLClassLoader create(String groupId, String artifactId, String version) {
        return create(MavenArtifact.of(groupId, artifactId, version));
    }

    /**
     * Creates a Maven Repository System for artifact resolution.
     */
    @SuppressWarnings("deprecation")
    private static RepositorySystem createRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                log.error("Service creation failed for type: {} with implementation: {}", type, impl, exception);
            }
        });

        return locator.getService(RepositorySystem.class);
    }

    /**
     * Creates a Repository System Session for artifact resolution.
     */
    private static RepositorySystemSession createRepositorySystemSession(RepositorySystem system) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        // Set local repository (default is ~/.m2/repository)
        String localRepoPath = System.getProperty("user.home") + "/.m2/repository";
        LocalRepository localRepo = new LocalRepository(localRepoPath);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

        return session;
    }
}
