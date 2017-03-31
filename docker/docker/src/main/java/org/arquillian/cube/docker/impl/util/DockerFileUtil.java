package org.arquillian.cube.docker.impl.util;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.arquillian.cube.containerobject.CubeDockerFile;
import org.arquillian.cube.impl.util.Which;

public class DockerFileUtil {

    private DockerFileUtil() {
        super();
    }

    public static void copyDockerfileDirectory(Class<?> containerObject, CubeDockerFile cubeDockerFile, File output) throws IOException {
        if (cubeDockerFile == null) {
            throw new IllegalArgumentException("No CubeDockerFile annotation is provided");
        }

        String dockerfilePath = resolveDockerfileLocation(containerObject, cubeDockerFile);
        copyDockerfileDirectoryFromClasspath(containerObject, dockerfilePath, output);
    }

    private static File createTemporaryFolder(Class<?> containerObject) throws IOException {
        File dir = File.createTempFile(containerObject.getSimpleName(), "Dockerfile");
        dir.delete();
        if(!dir.mkdirs()) {
            throw new IllegalStateException(
                    String.format("Directory %s for storing Dockerfile cannot be created.", dir));
        }

        return dir;
    }

    private static String resolveDockerfileLocation(Class<?> containerObject, CubeDockerFile cubeDockerFile) {
        String prefix = null;
        if(isSpecificDockerfileLocationSet(cubeDockerFile)) {
            prefix = cubeDockerFile.value();
        } else {
            prefix = containerObject.getName();
        }
        return prefix.replace('.', '/').replace('$', '/');
    }

    private static boolean isSpecificDockerfileLocationSet(CubeDockerFile cubeDockerFile) {
        return !cubeDockerFile.value().isEmpty();
    }

    private static void copyDockerfileDirectoryFromClasspath(Class<?> containerObject, String dockerfileLocation, File dir) throws IOException {
        File jar = null;
        try {
            jar = Which.jarFile(containerObject);
        } catch (IllegalArgumentException | IOException e) {
            throw new IllegalArgumentException(e);
        }

        if (jar!=null && jar.isFile()) {
            // files are packaged into a jar/war. extract them
            dockerfileLocation += "/";
            copyDockerfileDirectoryFromPackaged(jar, dockerfileLocation, dir);
        } else {
            // Dockerfile is not packaged into a jar file, so copy locally
            copyDockerfileDirectoryFromLocal(dockerfileLocation, dir);
        }
    }

    private static void copyDockerfileDirectoryFromPackaged(File jar, String location, File outputDirectory) throws IOException {
        try (JarFile j = new JarFile(jar)) {
            Enumeration<JarEntry> e = j.entries();
            while (e.hasMoreElements()) {
                JarEntry je = e.nextElement();
                if (je.getName().startsWith(location)) {
                    File dst = new File(outputDirectory, je.getName().substring(location.length()));
                    if (je.isDirectory()) {
                        dst.mkdirs();
                    } else {
                        try (InputStream in = j.getInputStream(je)) {
                            Files.copy(in, Paths.get(dst.toURI()));
                        }
                    }
                }
            }
        }
    }

    private static void copyDockerfileDirectoryFromLocal(String location, File outputDirectory) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resourceDir = classLoader.getResource(location);

        if (resourceDir == null) {
            throw new IllegalArgumentException(
                    String.format("No location found at %s", location)
            );
        }
        copyFile(outputDirectory, resourceDir);
    }

    private static void copyFile(File outputDirectory, URL resourceDir) throws IOException {
        File dockerFileDir;
        try {
            dockerFileDir = new File(resourceDir.toURI());
        } catch (URISyntaxException e) {
            dockerFileDir = new File(resourceDir.getPath());
        }
        FileUtils.copyDirectory(dockerFileDir, outputDirectory);
    }

}
