package org.arquillian.cube.impl.docker;

/**
 * User: BrunoGilbertCrane
 * Date: 20/01/15
 * Time: 5:33 PM
 */

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.when;

import org.arquillian.cube.impl.client.CubeConfiguration;
import org.arquillian.cube.impl.util.CommandLineExecutor;
import org.arquillian.cube.impl.util.IOUtil;
import org.arquillian.cube.impl.util.OperatingSystem;
import org.arquillian.cube.impl.util.OperatingSystemResolver;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.kenai.jnr.x86asm.OP;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RunWith(MockitoJUnitRunner.class)
public class DockerClientExecutorTest {

    private static final Pattern HEXA_PATTERN = Pattern.compile("^[A-Fa-f0-9]+$");
    public static final String FULL_LOG = "FROM ubuntu:12.04 ---> f959d044ebdfStep 1 : MAINTAINER Bruno Crane BrunoGilbertCrane@users.noreply.github.com ---> " +
            "Running in dfda3b1da834 ---> f3829d9d380fRemoving intermediate container dfda3b1da834Step 2 : ENV JAVA_HOME /usr/lib/jvm/java-7-openjdk-amd64 ---> " +
            "Running in 0a6b82603339 ---> 92164ef3eeb0Removing intermediate container 0a6b82603339Step 3 : ENV JAVA_OPTS -Dcom.sun.management.jmxremote.port=8089 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false ---> " +
            "Running in d2bab4be218f ---> 5fbf84cc7b1eRemoving intermediate container d2bab4be218fStep 4 : ENV CATALINA_OPTS -Dtomee.host=localhost -Dtomee.http=8081 -Dtomee.shutdown=8015 ---> " +
            "Running in 68c1f0f19014 ---> 61532db01348Removing intermediate container 68c1f0f19014Step 5 : ADD apache-tomee-plus-1.7.1.tar.gz /opt/apache-tomee-plus-1.7.1 ---> 53e8e145f5e0Removing intermediate container 2cf5f4eba0a4Step 6 : WORKDIR /opt/apache-tomee-plus-1.7.1/webapps/ ---> " +
            "Running in 1635c0c6144e ---> 11734dae26a5Removing intermediate container 1635c0c6144eStep 7 : COPY mondrian.war /opt/apache-tomee-plus-1.7.1/webapps/mondrian.war ---> 42634d0d4d5aRemoving intermediate container 610fcecfd374Step 8 : EXPOSE 8081 8015 ---> " +
            "Running in a168700091f6 ---> 31c3e9716369Removing intermediate container a168700091f6Step 9 : CMD /opt/apache-tomee-plus-1.7.1/webapps/bin/catalina.sh run ---> " +
            "Running in 4603bb82ee2c ---> fcff4d43f77aRemoving intermediate container 4603bb82ee2cSuccessfully built fcff4d43f77a";

    @Mock
    private CommandLineExecutor commandLineExecutor;

    @Mock
    private OperatingSystemResolver operatingSystemResolver;

    @Test
    public void testGetImageIdFailed() throws Exception {
        String imageId = IOUtil.substringBetween(FULL_LOG, "Successfully built ", "\\n\"}");
        Assert.assertNull("IOUtil.substringBetween do the job", imageId);
    }

    @Test
    public void testGetImageId() throws Exception {
        String imageId = DockerClientExecutor.getImageId(FULL_LOG);
        Assert.assertNotNull("Pattern matching failed", imageId);

        // All images are identified by a 64 hexadecimal digit string
        // https://docs.docker.com/terms/image/
        Matcher m = HEXA_PATTERN.matcher(imageId);
        Assert.assertTrue("imageId is not an hexadecimal digit string", m.matches());

    }

    @Test
    public void shouldExecuteBoot2Docker() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("serverVersion", "1.12");
        map.put("serverUri", "http://boot2docker:2376");
        map.put("boot2dockerPath", "/opt/boot2docker/boot2docker");
        CubeConfiguration cubeConfiguration =
            CubeConfiguration.fromMap(map);

        when(commandLineExecutor.execCommand("/opt/boot2docker/boot2docker")).thenReturn("The VM's Host only interface IP address is: 192.168.59.103");

        DockerClientExecutor dockerClientExecutor =
                new DockerClientExecutor(cubeConfiguration, commandLineExecutor, operatingSystemResolver);
        assertThat(dockerClientExecutor.getDockerUri().getHost(), is("192.168.59.103"));
    }

    @Test
    public void shouldGetDefaultUnixSocketIfNoServerUriUnderLinux() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("serverVersion", "1.12");
        CubeConfiguration cubeConfiguration =
            CubeConfiguration.fromMap(map);

        when(operatingSystemResolver.currentOperatingSystem()).thenReturn(OperatingSystem.LINUX_OS);

        DockerClientExecutor dockerClientExecutor =
                new DockerClientExecutor(cubeConfiguration, commandLineExecutor, operatingSystemResolver);
        assertThat(dockerClientExecutor.getDockerUri(), is(URI.create("unix:///var/run/docker.sock")));
    }

    @Test
    public void shouldGetDefaultBoot2DockerIfNoServerUriUnderWindows() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("serverVersion", "1.12");
        map.put("boot2dockerPath", "/opt/boot2docker/boot2docker");
        CubeConfiguration cubeConfiguration =
            CubeConfiguration.fromMap(map);

        when(commandLineExecutor.execCommand("/opt/boot2docker/boot2docker")).thenReturn("The VM's Host only interface IP address is: 192.168.59.103");
        when(operatingSystemResolver.currentOperatingSystem()).thenReturn(OperatingSystem.WINDOWS_7);

        DockerClientExecutor dockerClientExecutor =
                new DockerClientExecutor(cubeConfiguration, commandLineExecutor, operatingSystemResolver);
        assertThat(dockerClientExecutor.getDockerUri().getHost(), is("192.168.59.103"));
    }

    @Test
    public void shouldGetDefaultBoot2DockerIfNoServerUriUnderMacOS() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("serverVersion", "1.12");
        map.put("boot2dockerPath", "/opt/boot2docker/boot2docker");
        CubeConfiguration cubeConfiguration =
            CubeConfiguration.fromMap(map);

        when(commandLineExecutor.execCommand("/opt/boot2docker/boot2docker")).thenReturn("The VM's Host only interface IP address is: 192.168.59.103");
        when(operatingSystemResolver.currentOperatingSystem()).thenReturn(OperatingSystem.MAC_OSX);

        DockerClientExecutor dockerClientExecutor =
                new DockerClientExecutor(cubeConfiguration, commandLineExecutor, operatingSystemResolver);
        assertThat(dockerClientExecutor.getDockerUri().getHost(), is("192.168.59.103"));
    }

}
