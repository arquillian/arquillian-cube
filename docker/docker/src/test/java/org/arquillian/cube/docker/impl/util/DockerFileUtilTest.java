package org.arquillian.cube.docker.impl.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import org.arquillian.cube.containerobject.CubeDockerFile;
import org.hamcrest.core.Is;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DockerFileUtilTest {


    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void shouldCopyDockerfileFromDefaultLocation() throws IOException {
        File outputDir = folder.newFolder();

        DockerFileUtil.copyDockerfileDirectory(TestLocalDockerFile.class, TestLocalDockerFile.class.getAnnotation(CubeDockerFile.class), outputDir);
        Assert.assertThat(new File(outputDir, "Dockerfile").exists(), Is.is(true));
    }

    @Test
    public void shouldCopyDockerfileFromDefaultJar() throws IOException, ClassNotFoundException {

        //Creates a jar file with required content
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class)
                .addClass(TestJarDockerFile.class)
                .addClasses(CubeDockerFile.class)
                .addAsResource(
                        new StringAsset("FROM java:8-jre"),
                        "/org/arquillian/cube/docker/impl/util/DockerFileUtilTest/TestJarDockerFile/Dockerfile"
                );
        File jarDirectory = folder.newFolder();
        File jarFile = new File(jarDirectory, "test.jar");
        jar.as(ZipExporter.class).exportTo(jarFile);

        //Creates a class loader that depends on the system one loading the jar. Jars are isolated from the test ones.
        ClassLoader classloader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()}, null);

        Class<?> clazz = (Class<?>) Class.forName("org.arquillian.cube.docker.impl.util.DockerFileUtilTest$TestJarDockerFile", true, classloader);

        //Executes the test
        File outputDir = folder.newFolder();

        DockerFileUtil.copyDockerfileDirectory(clazz, TestJarDockerFile.class.getAnnotation(CubeDockerFile.class), outputDir);
        Assert.assertThat(new File(outputDir, "Dockerfile").exists(), Is.is(true));
    }

    @Test
    public void shouldCopyDockerfileFromJarLocation() throws IOException, ClassNotFoundException {

        //Creates a jar file with required content
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class)
                .addClass(CustomTestJarDockerfiler.class)
                .addClasses(CubeDockerFile.class)
                .addAsResource(
                        new StringAsset("FROM java:8-jre"),
                        "/test/Dockerfile"
                );
        File jarDirectory = folder.newFolder();
        File jarFile = new File(jarDirectory, "test.jar");
        jar.as(ZipExporter.class).exportTo(jarFile);

        //Creates a class loader that depends on the system one loading the jar. Jars are isolated from the test ones.
        ClassLoader classloader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()}, null);

        Class<?> clazz = (Class<?>) Class.forName("org.arquillian.cube.docker.impl.util.DockerFileUtilTest$CustomTestJarDockerfiler", true, classloader);

        //Executes the test
        File outputDir = folder.newFolder();

        DockerFileUtil.copyDockerfileDirectory(clazz, CustomTestJarDockerfiler.class.getAnnotation(CubeDockerFile.class), outputDir);
        Assert.assertThat(new File(outputDir, "Dockerfile").exists(), Is.is(true));
    }

    @CubeDockerFile
    public static class TestLocalDockerFile {
    }

    @CubeDockerFile
    public static class TestJarDockerFile {
    }

    @CubeDockerFile("test")
    public static class CustomTestJarDockerfiler  {
    }

}
