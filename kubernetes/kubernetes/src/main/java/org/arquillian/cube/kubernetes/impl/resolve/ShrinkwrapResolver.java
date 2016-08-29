package org.arquillian.cube.kubernetes.impl.resolve;

import org.arquillian.cube.impl.util.IOUtil;
import org.arquillian.cube.kubernetes.api.DependencyResolver;
import org.arquillian.cube.kubernetes.api.Logger;
import org.arquillian.cube.kubernetes.api.Session;
import org.arquillian.cube.kubernetes.impl.log.SimpleLogger;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;


import static org.arquillian.cube.kubernetes.api.Configuration.DEFAULT_CONFIG_FILE_NAME;

public class ShrinkwrapResolver implements DependencyResolver {

    public static final String JAR = "jar";
    public static final String JSON_SUFFIX= ".json";
    public static final String DEFAULT_PATH_TO_POM = "pom.xml";

    private final String pathToPomFile;
    private final boolean rethrowExceptions;

    public ShrinkwrapResolver() {
        this(DEFAULT_PATH_TO_POM, false);
    }

    //Mostly needed for testing
    public ShrinkwrapResolver(String pathToPomFile, boolean rethrowExceptions) {
        this.pathToPomFile = pathToPomFile;
        this.rethrowExceptions = rethrowExceptions;
    }

    public List<URL> resolve(Session session) throws IOException {
        List<URL> dependencies = new ArrayList<>();
        try {
            File[] files = Maven.resolver().loadPomFromFile(pathToPomFile).importTestDependencies().resolve().withoutTransitivity().asFile();
            for (File f : files) {
                if (f.getName().endsWith(JAR) && hasKubernetesJson(f)) {
                    Path dir = Files.createTempDirectory(session.getId());
                    try (FileInputStream fis = new FileInputStream(f); JarInputStream jis = new JarInputStream(fis)) {
                        IOUtil.unzip(new FileInputStream(f), dir.toFile());
                        File jsonPath = dir.resolve(DEFAULT_CONFIG_FILE_NAME).toFile();
                        if (jsonPath.exists()) {
                            dependencies.add(jsonPath.toURI().toURL());
                        }
                    }
                } else if (f.getName().endsWith(JSON_SUFFIX)) {
                    dependencies.add(f.toURI().toURL());
                }
            }
        } catch (Exception e) {
            if (rethrowExceptions) {
                throw e;
            } else {
                session.getLogger().warn("Skipping maven project dependencies. Caused by:" + e.getMessage());
            }
        }
        return dependencies;
    }

    private boolean hasKubernetesJson(File f) throws IOException {
        return hasResource(f, DEFAULT_CONFIG_FILE_NAME);
    }


    private boolean hasResource(File f, String name) throws IOException {
        try (FileInputStream fis = new FileInputStream(f); JarInputStream jis = new JarInputStream(fis)) {
            for (JarEntry entry = jis.getNextJarEntry(); entry != null; entry = jis.getNextJarEntry()) {
                if (entry.getName().equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }
}
