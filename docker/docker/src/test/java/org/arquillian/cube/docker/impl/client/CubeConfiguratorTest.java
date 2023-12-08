package org.arquillian.cube.docker.impl.client;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.SystemUtils;
import org.arquillian.cube.docker.impl.util.Boot2Docker;
import org.arquillian.cube.docker.impl.util.CommandLineExecutor;
import org.arquillian.cube.docker.impl.util.DockerMachine;
import org.arquillian.cube.docker.impl.util.OperatingSystemFamily;
import org.arquillian.cube.docker.impl.util.OperatingSystemFamilyInterface;
import org.arquillian.cube.docker.impl.util.OperatingSystemInterface;
import org.arquillian.cube.docker.impl.util.OperatingSystemResolver;
import org.arquillian.cube.docker.impl.util.Top;
import org.arquillian.cube.spi.CubeConfiguration;
import org.hamcrest.Matcher;
import org.hamcrest.core.StringEndsWith;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.config.descriptor.api.ExtensionDef;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.test.AbstractManagerTestBase;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CubeConfiguratorTest extends AbstractManagerTestBase {
    
    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Mock
    CommandLineExecutor commandLineExecutor;
    @Mock
    ArquillianDescriptor arquillianDescriptor;
    @Mock
    ExtensionDef extensionDef;
    @Mock
    OperatingSystemInterface operatingSystem;
    @Mock
    OperatingSystemFamilyInterface operatingSystemFamily;
    @Mock
    Top top;

    private static Matcher<String> defaultDockerMachineCertPath() {
        return containsString(".docker" + File.separator + "machine" + File.separator + "machines");
    }

    private static Matcher<String> defaultBootToDockerCertPath() {
        return containsString(".boot2docker" + File.separator + "certs");
    }

    private static Matcher<String> pathEndsWith(String suffix) {
        return new PathStringEndsWithMatcher(suffix);
    }

    private void bindNonExistingDockerSocketOS() {
        lenient().when(operatingSystem.getDefaultFamily()).thenReturn(operatingSystemFamily);
        lenient().when(operatingSystem.getFamily()).thenReturn(OperatingSystemFamily.MAC);
        when(operatingSystemFamily.getServerUri()).thenReturn("non/existing/path");

        bind(ApplicationScoped.class, OperatingSystemInterface.class, operatingSystem);
    }

    @Override
    protected void addExtensions(List<Class<?>> extensions) {
        extensions.add(CubeDockerConfigurator.class);
        super.addExtensions(extensions);
    }

    @Before
    public void setup() {
        bind(ApplicationScoped.class, Boot2Docker.class, new Boot2Docker(commandLineExecutor));
        bind(ApplicationScoped.class, DockerMachine.class, new DockerMachine(commandLineExecutor));
        bind(ApplicationScoped.class, ArquillianDescriptor.class, arquillianDescriptor);
        bind(ApplicationScoped.class, Top.class, top);

        when(top.isSpinning()).thenReturn(false);
        
        environmentVariables.clear("DOCKER_HOST", "DOCKER_MACHINE_NAME", "DOCKER_TLS_VERIFY", "DOCKER_CERT_PATH");
    }

    @Test
    public void shouldChangeServerUriInCaseOfRunningDockerInsideDocker() {
        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "https://dockerHost:22222");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        lenient().when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
        lenient().when(commandLineExecutor.execCommand("boot2docker", "ip")).thenReturn("192.168.0.1");
        when(commandLineExecutor.execCommand("docker-machine")).thenThrow(new RuntimeException());
        when(top.isSpinning()).thenReturn(true);

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, OperatingSystemFamily.DIND.getServerUri()));
    }

    @Test
    public void shouldNotChangeServerUriInCaseODockerInsideDockerIfItIsDisabled() {
        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "tcp://dockerHost:22222");
        config.put(CubeDockerConfiguration.DIND_RESOLUTION, "false");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
        when(commandLineExecutor.execCommand("boot2docker", "ip")).thenReturn("192.168.0.1");
        when(commandLineExecutor.execCommand("docker-machine")).thenThrow(new RuntimeException());

        lenient().when(top.isSpinning()).thenReturn(true);

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "tcp://192.168.0.1:22222"));
    }

    @Test
    public void shouldUseBoot2DockerIfDockerHostIsSetOnServerURIByDefault() {

        assumeThat(new OperatingSystemResolver().currentOperatingSystem().getFamily(),
            is(not(OperatingSystemFamily.LINUX)));

        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "tcp://dockerHost:22222");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
        when(commandLineExecutor.execCommand("boot2docker", "ip")).thenReturn("192.168.0.1");
        when(commandLineExecutor.execCommand("docker-machine")).thenThrow(new RuntimeException());

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "tcp://192.168.0.1:22222"));
        assertThat(config, hasEntry(is(CubeDockerConfiguration.CERT_PATH), defaultBootToDockerCertPath()));
    }

    @Test
    public void shouldUseDockerMachineIfDockerHostIsSetOnServerURIAndMachineNameIsSet() {
        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "tcp://dockerHost:22222");
        config.put(CubeDockerConfiguration.DOCKER_MACHINE_NAME, "dev");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
        when(commandLineExecutor.execCommandAsArray("docker-machine", "ls"))
            .thenReturn(Arrays.asList(
                "NAME   ACTIVE   DRIVER       STATE     URL                         SWARM",
                "dev    *        virtualbox   Running   tcp://192.168.0.2:222222     "));
        when(commandLineExecutor.execCommand("docker-machine", "ip", "dev")).thenReturn("192.168.0.2");

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "tcp://192.168.0.2:22222"));
    }

    @Test
    public void shouldStartDockerMachineIfItIsStoppedAndMachineNameIsSet() {
        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "tcp://dockerHost:22222");
        config.put(CubeDockerConfiguration.DOCKER_MACHINE_NAME, "dev");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
        when(commandLineExecutor.execCommandAsArray("docker-machine", "ls"))
            .thenReturn(Arrays.asList(
                "NAME   ACTIVE   DRIVER       STATE     URL                         SWARM",
                "dev    *        virtualbox   Stopped   tcp://192.168.0.2:222222     "));
        when(commandLineExecutor.execCommand("docker-machine", "ip", "dev")).thenReturn("192.168.0.2");

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "tcp://192.168.0.2:22222"));
        verify(commandLineExecutor, times(1)).execCommand("docker-machine", "start", "dev");
    }

    @Test
    public void shouldUseDockerMachineIfDockerHostIsNotSetAndOnlyOneMachineIsRunning() {
        Map<String, String> config = new HashMap<>();

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
        when(commandLineExecutor.execCommand("docker-machine", "ip", "dev")).thenReturn("192.168.99.100");

        when(commandLineExecutor.execCommandAsArray("docker-machine", "ls", "--filter", "state=Running"))
            .thenReturn(Arrays.asList(
                "NAME   ACTIVE   DRIVER       STATE     URL                         SWARM",
                "dev    *        virtualbox   Running   tcp://192.168.99.100:2376     "));

        when(commandLineExecutor.execCommandAsArray("docker-machine", "ls"))
            .thenReturn(Arrays.asList(
                "NAME   ACTIVE   DRIVER       STATE     URL                         SWARM",
                "dev    *        virtualbox   Running   tcp://192.168.99.100:2376     "));
        // Docker Machine is installed
        when(commandLineExecutor.execCommand("docker-machine"))
            .thenReturn("Usage: docker-machine [OPTIONS] COMMAND [arg...]");

        bindNonExistingDockerSocketOS();

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "tcp://192.168.99.100:2376"));
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_MACHINE_NAME, "dev"));
    }

    // Only works in case of running in MACOS or Windows since by default in
    // these systems boot2docker is the default
    @Test
    public void shouldNotUseDockerMachineIfDockerHostIsNotSetNotDockerMachineAndTwoMachineIsRunning() {

        Assume.assumeTrue(SystemUtils.IS_OS_MAC_OSX || SystemUtils.IS_OS_WINDOWS);

        Map<String, String> config = new HashMap<>();

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
        when(commandLineExecutor.execCommand("docker-machine", "ip", "dev")).thenReturn("192.168.99.100");
        when(commandLineExecutor.execCommandAsArray("docker-machine", "ls"))
            .thenReturn(Arrays.asList(
                "NAME   ACTIVE   DRIVER       STATE     URL                         SWARM",
                "dev    *        virtualbox   Running   tcp://192.168.99.100:2376     ",
                "dev2   *        virtualbox   Running   tcp://192.168.99.100:2376     "));
        when(commandLineExecutor.execCommand("boot2docker", "ip")).thenReturn("192.168.0.1");
        // Docker-Machine is installed
        when(commandLineExecutor.execCommand("docker-machine"))
            .thenReturn("Usage: docker-machine [OPTIONS] COMMAND [arg...]");


        bindNonExistingDockerSocketOS();

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "tcp://192.168.0.1:2376"));
    }

    // Only works in case of running in MACOS or Windows since by default in
    // these systems boot2docker is the default
    @Test
    public void shouldUseDefaultsInCaseOfNotHavingDockerMachineInstalledAndNoDockerUriNorMachineName() {

        Assume.assumeTrue(SystemUtils.IS_OS_MAC_OSX || SystemUtils.IS_OS_WINDOWS);

        Map<String, String> config = new HashMap<>();

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);

        when(commandLineExecutor.execCommand("boot2docker", "ip")).thenReturn("192.168.0.1");
        when(commandLineExecutor.execCommand("docker-machine")).thenThrow(new IllegalArgumentException());

        bindNonExistingDockerSocketOS();

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "tcp://192.168.0.1:2376"));
    }

    @Test
    public void shouldSetServerIpWithLocalhostInCaseOfNativeLinuxInstallation() {
        String originalVar = System.getProperty(CubeDockerConfigurator.DOCKER_HOST);
        try {
            System.setProperty(CubeDockerConfigurator.DOCKER_HOST, "unix:///var/run/docker.sock");

            Map<String, String> config = new HashMap<>();

            when(extensionDef.getExtensionProperties()).thenReturn(config);
            when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
            when(commandLineExecutor.execCommand("docker-machine")).thenThrow(new RuntimeException());

            fire(new CubeConfiguration());
            assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "unix:///var/run/docker.sock"));
            assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_SERVER_IP, "localhost"));
        } finally {
            if (originalVar != null) {
                System.setProperty(CubeDockerConfigurator.DOCKER_HOST, originalVar);
            } else {
                System.clearProperty(CubeDockerConfigurator.DOCKER_HOST);
            }
        }
    }

    @Test
    public void shouldUseHostEnvIfDockerHostIsSetOnServerURIAndSystemEnvironmentVarIsSet() {
        String originalVar = System.getProperty(CubeDockerConfigurator.DOCKER_HOST);
        try {
            System.setProperty(CubeDockerConfigurator.DOCKER_HOST, "tcp://127.0.0.1:22222");

            Map<String, String> config = new HashMap<>();

            when(extensionDef.getExtensionProperties()).thenReturn(config);
            when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
            when(commandLineExecutor.execCommand("docker-machine")).thenThrow(new RuntimeException());

            fire(new CubeConfiguration());
            assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "tcp://127.0.0.1:22222"));
            assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_SERVER_IP, "127.0.0.1"));
        } finally {
            if (originalVar != null) {
                System.setProperty(CubeDockerConfigurator.DOCKER_HOST, originalVar);
            } else {
                System.clearProperty(CubeDockerConfigurator.DOCKER_HOST);
            }
        }
    }

    @Test
    public void tlsVerifyShouldBeTrueInCaseOfDockerMachine() {
        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "https://dockerHost:22222");
        config.put(CubeDockerConfiguration.DOCKER_MACHINE_NAME, "dev");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
        when(commandLineExecutor.execCommand("docker-machine", "ip", "dev")).thenReturn("192.168.0.2");
        when(commandLineExecutor.execCommandAsArray("docker-machine", "ls"))
            .thenReturn(Arrays.asList(
                "NAME   ACTIVE   DRIVER       STATE     URL                         SWARM",
                "dev    *        virtualbox   Running   tcp://192.168.0.2:222222     "));

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "tcp://192.168.0.2:22222"));
        assertThat(config, hasEntry(CubeDockerConfiguration.TLS_VERIFY, "true"));
        assertThat(config, hasEntry(is(CubeDockerConfiguration.CERT_PATH), defaultDockerMachineCertPath()));
    }

    @Test
    public void tlsVerifyShouldBeTrueInCaseOfSetToFalseAndDockerHostTagNotPresent() {
        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "tcp://192.168.0.2:22222");
        config.put(CubeDockerConfiguration.TLS_VERIFY, "false");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
        when(commandLineExecutor.execCommand("docker-machine")).thenThrow(new RuntimeException());

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "tcp://192.168.0.2:22222"));
        assertThat(config, hasEntry(CubeDockerConfiguration.TLS_VERIFY, "false"));
        assertThat(config, not(hasKey(CubeDockerConfiguration.CERT_PATH)));
    }

    @Test
    public void tlsVerifyShouldBeFalseInCaseOfSetToFalseAndDockerHostTagPresent() {
        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "tcp://dockerHost:22222");
        config.put(CubeDockerConfiguration.TLS_VERIFY, "false");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
        when(commandLineExecutor.execCommand("boot2docker", "ip")).thenReturn("192.168.0.2");
        when(commandLineExecutor.execCommand("docker-machine")).thenThrow(new RuntimeException());
        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "tcp://192.168.0.2:22222"));
        assertThat(config, hasEntry(CubeDockerConfiguration.TLS_VERIFY, "false"));
        assertThat(config, not(hasKey(CubeDockerConfiguration.CERT_PATH)));
    }

    @Test
    public void tlsVerifyShouldBeTrueInCaseOfNotSetAndDockerHostTagPresent() {

        assumeThat(new OperatingSystemResolver().currentOperatingSystem().getFamily(),
            is(not(OperatingSystemFamily.LINUX)));

        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "tcp://dockerHost:22222");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
        when(commandLineExecutor.execCommand("boot2docker", "ip")).thenReturn("192.168.0.2");
        when(commandLineExecutor.execCommand("docker-machine")).thenThrow(new RuntimeException());

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "tcp://192.168.0.2:22222"));
        assertThat(config, hasEntry(CubeDockerConfiguration.TLS_VERIFY, "true"));
        assertThat(config, hasEntry(is(CubeDockerConfiguration.CERT_PATH), defaultBootToDockerCertPath()));
    }

    @Test
    public void tlsVerifyShouldBeTrueInCaseOfHttpsServerUri() {
        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "https://192.168.0.2:22222");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
        when(commandLineExecutor.execCommand("docker-machine")).thenThrow(new RuntimeException());

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "tcp://192.168.0.2:22222"));
        assertThat(config, hasEntry(CubeDockerConfiguration.TLS_VERIFY, "true"));
        assertThat(config, hasEntry(is(CubeDockerConfiguration.CERT_PATH), defaultBootToDockerCertPath()));
    }

    @Test
    public void tlsVerifyShouldBeTrueInCaseOfSetToFalseAndHttpsServerUri() {
        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "https://192.168.0.2:22222");
        config.put(CubeDockerConfiguration.TLS_VERIFY, "false");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
        when(commandLineExecutor.execCommand("docker-machine")).thenThrow(new RuntimeException());
        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "tcp://192.168.0.2:22222"));
        assertThat(config, hasEntry(CubeDockerConfiguration.TLS_VERIFY, "true"));
        assertThat(config, hasEntry(is(CubeDockerConfiguration.CERT_PATH), defaultBootToDockerCertPath()));
    }

    @Test
    public void tlsVerifyShouldBeFalseInCaseOfHttpServerUri() throws Exception {
        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "http://192.168.0.2:22222");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
        when(commandLineExecutor.execCommand("docker-machine")).thenThrow(new RuntimeException());

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "tcp://192.168.0.2:22222"));
        assertThat(config, hasEntry(CubeDockerConfiguration.TLS_VERIFY, "false"));
        assertThat(config, not(hasKey(CubeDockerConfiguration.CERT_PATH)));
    }

    @Test
    public void tlsVerifyShouldBeFalseInCaseOfSetToTrueAndHttpServerUri() {
        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "http://192.168.0.2:22222");
        config.put(CubeDockerConfiguration.TLS_VERIFY, "true");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
        when(commandLineExecutor.execCommand("docker-machine")).thenThrow(new RuntimeException());

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "tcp://192.168.0.2:22222"));
        assertThat(config, hasEntry(CubeDockerConfiguration.TLS_VERIFY, "false"));
        assertThat(config, not(hasKey(CubeDockerConfiguration.CERT_PATH)));
    }

    @Test
    public void dockerUriConfigurationParameterShouldTakePrecedenceOverSystemEnv() {

        assumeThat(new OperatingSystemResolver().currentOperatingSystem().getFamily(),
            is(not(OperatingSystemFamily.LINUX)));

        String originalVar = System.getProperty(CubeDockerConfigurator.DOCKER_HOST);
        try {
            System.setProperty(CubeDockerConfigurator.DOCKER_HOST, "tcp://127.0.0.1:22222");

            Map<String, String> config = new HashMap<>();
            config.put(CubeDockerConfiguration.DOCKER_URI, "tcp://dockerHost:22222");

            when(extensionDef.getExtensionProperties()).thenReturn(config);
            when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);

            when(extensionDef.getExtensionProperties()).thenReturn(config);
            when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
            when(commandLineExecutor.execCommand("boot2docker", "ip")).thenReturn("192.168.0.1");
            when(commandLineExecutor.execCommand("docker-machine")).thenThrow(new RuntimeException());

            fire(new CubeConfiguration());
            assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "tcp://192.168.0.1:22222"));
            assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_SERVER_IP, "192.168.0.1"));
        } finally {
            if (originalVar != null) {
                System.setProperty(CubeDockerConfigurator.DOCKER_HOST, originalVar);
            } else {
                System.clearProperty(CubeDockerConfigurator.DOCKER_HOST);
            }
        }
    }

    @Test
    public void shouldDumpCubeConfiguration() throws Exception {
        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "https://dockerHost:22222");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
        when(commandLineExecutor.execCommand("boot2docker", "ip")).thenReturn("192.168.0.1");
        when(commandLineExecutor.execCommand("docker-machine")).thenThrow(new RuntimeException());

        PrintStream old = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        CubeConfiguration cubeConfiguration = new CubeConfiguration();
        fire(cubeConfiguration);
        System.out.flush();
        System.setOut(old);
        assertThat(baos.toString(), containsString("CubeDockerConfiguration:"));
    }

    private static class PathStringEndsWithMatcher extends StringEndsWith {

        public PathStringEndsWithMatcher(String suffix) {
            super(suffix);
        }

        @Override
        protected boolean evalSubstringOf(String s) {
            return Paths.get(s).endsWith(substring);
        }
    }
}
