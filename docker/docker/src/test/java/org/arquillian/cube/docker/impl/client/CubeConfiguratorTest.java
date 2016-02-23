package org.arquillian.cube.docker.impl.client;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.SystemUtils;
import org.arquillian.cube.docker.impl.util.Boot2Docker;
import org.arquillian.cube.docker.impl.util.CommandLineExecutor;
import org.arquillian.cube.docker.impl.util.DockerMachine;
import org.arquillian.cube.docker.impl.util.OperatingSystemFamily;
import org.arquillian.cube.docker.impl.util.Top;
import org.arquillian.cube.spi.CubeConfiguration;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.config.descriptor.api.ExtensionDef;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.test.AbstractManagerTestBase;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CubeConfiguratorTest extends AbstractManagerTestBase {

    @Override
    protected void addExtensions(List<Class<?>> extensions) {
        extensions.add(CubeDockerConfigurator.class);
        super.addExtensions(extensions);
    }

    @Mock
    CommandLineExecutor commandLineExecutor;

    @Mock
    ArquillianDescriptor arquillianDescriptor;
    @Mock
    ExtensionDef extensionDef;
    @Mock
    Top top;

    @Before
    public void setup() {
        bind(ApplicationScoped.class, Boot2Docker.class, new Boot2Docker(commandLineExecutor));
        bind(ApplicationScoped.class, DockerMachine.class, new DockerMachine(commandLineExecutor));
        bind(ApplicationScoped.class, ArquillianDescriptor.class, arquillianDescriptor);
        bind(ApplicationScoped.class, Top.class, top);
        when(top.isSpinning()).thenReturn(false);
    }

    @Test
    public void shouldChangeServerUriInCaseOfRunningDockerInsideDocker() {
        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "https://dockerHost:22222");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
        when(commandLineExecutor.execCommand("boot2docker", "ip")).thenReturn("192.168.0.1");

        when(top.isSpinning()).thenReturn(true);

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, OperatingSystemFamily.DIND.getServerUri()));
    }

    @Test
    public void shouldNotChangeServerUriInCaseODockerInsideDockerIfItIsDisabled() {
        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "https://dockerHost:22222");
        config.put(CubeDockerConfiguration.DIND_RESOLUTION, "false");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
        when(commandLineExecutor.execCommand("boot2docker", "ip")).thenReturn("192.168.0.1");

        when(top.isSpinning()).thenReturn(true);

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "https://192.168.0.1:22222"));
    }

    @Test
    public void shouldUseBoot2DockerIfDockerHostIsSetOnServerURIByDefault() {
        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "https://dockerHost:22222");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
        when(commandLineExecutor.execCommand("boot2docker", "ip")).thenReturn("192.168.0.1");

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "https://192.168.0.1:22222"));
        assertThat(config, hasEntry(is(CubeDockerConfiguration.CERT_PATH), endsWith(
                File.separator + ".boot2docker" + File.separator + "certs" + File.separator + "boot2docker-vm")));
    }

    @Test
    public void shouldUseDockerMachineIfDockerHostIsSetOnServerURIAndMachineNameIsSet() {
        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "https://dockerHost:22222");
        config.put(CubeDockerConfiguration.DOCKER_MACHINE_NAME, "dev");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
        when(commandLineExecutor.execCommandAsArray("docker-machine", "ls", "--filter", "name=dev"))
                .thenReturn(Arrays.asList(
                        "NAME   ACTIVE   DRIVER       STATE     URL                         SWARM",
                        "dev    *        virtualbox   Running   tcp://192.168.0.2:222222     " ));
        when(commandLineExecutor.execCommand("docker-machine", "ip", "dev")).thenReturn("192.168.0.2");

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "https://192.168.0.2:22222"));
        assertThat(config,
                hasEntry(is(CubeDockerConfiguration.CERT_PATH),
                        endsWith(File.separator + ".docker" + File.separator + "machine" + File.separator + "machines"
                                + File.separator + config.get(CubeDockerConfiguration.DOCKER_MACHINE_NAME))));

    }

    @Test
    public void shouldStartDockerMachineIfItIsStoppedAndMachineNameIsSet() {
        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "https://dockerHost:22222");
        config.put(CubeDockerConfiguration.DOCKER_MACHINE_NAME, "dev");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
        when(commandLineExecutor.execCommandAsArray("docker-machine", "ls", "--filter", "name=dev"))
                .thenReturn(Arrays.asList(
                        "NAME   ACTIVE   DRIVER       STATE     URL                         SWARM",
                        "dev    *        virtualbox   Stopped   tcp://192.168.0.2:222222     " ));
        when(commandLineExecutor.execCommand("docker-machine", "ip", "dev")).thenReturn("192.168.0.2");

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "https://192.168.0.2:22222"));
        assertThat(config,
                hasEntry(is(CubeDockerConfiguration.CERT_PATH),
                        endsWith(File.separator + ".docker" + File.separator + "machine" + File.separator + "machines"
                                + File.separator + config.get(CubeDockerConfiguration.DOCKER_MACHINE_NAME))));
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
                        "dev    *        virtualbox   Running   tcp://192.168.99.100:2376     " ));
        // Docker Machine is installed
        when(commandLineExecutor.execCommand("docker-machine"))
                .thenReturn("Usage: docker-machine [OPTIONS] COMMAND [arg...]");

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "https://192.168.99.100:2376"));
        assertThat(config,
                hasEntry(is(CubeDockerConfiguration.CERT_PATH),
                        endsWith(File.separator + ".docker" + File.separator + "machine" + File.separator + "machines"
                                + File.separator + config.get(CubeDockerConfiguration.DOCKER_MACHINE_NAME))));
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
        when(commandLineExecutor.execCommandAsArray("docker-machine", "ls", "--filter", "state=Running"))
                .thenReturn(Arrays.asList(
                        "NAME   ACTIVE   DRIVER       STATE     URL                         SWARM",
                        "dev    *        virtualbox   Running   tcp://192.168.99.100:2376     ",
                        "dev2   *        virtualbox   Running   tcp://192.168.99.100:2376     " ));
        when(commandLineExecutor.execCommand("boot2docker", "ip")).thenReturn("192.168.0.1");
        // Docker-Machine is installed
        when(commandLineExecutor.execCommand("docker-machine"))
                .thenReturn("Usage: docker-machine [OPTIONS] COMMAND [arg...]");

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "https://192.168.0.1:2376"));

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

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "https://192.168.0.1:2376"));
    }

    @Test
    public void shouldUseHostEnvIfDockerHostIsSetOnServerURIAndSystemEnvironmentVarIsSet() {
        String originalVar = System.getProperty(CubeDockerConfigurator.DOCKER_HOST);
        try {
            System.setProperty(CubeDockerConfigurator.DOCKER_HOST, "tcp://127.0.0.1:22222");

            Map<String, String> config = new HashMap<>();

            when(extensionDef.getExtensionProperties()).thenReturn(config);
            when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);

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
    public void dockerUriTcpShouldBeReplacedToHttpsInCaseOfDockerMachine() {
        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "tcp://dockerHost:22222");
        config.put(CubeDockerConfiguration.DOCKER_MACHINE_NAME, "dev");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
        when(commandLineExecutor.execCommand("docker-machine", "ip", "dev")).thenReturn("192.168.0.2");
        when(commandLineExecutor.execCommandAsArray("docker-machine", "ls", "--filter", "name=dev"))
                .thenReturn(Arrays.asList(
                        "NAME   ACTIVE   DRIVER       STATE     URL                         SWARM",
                        "dev    *        virtualbox   Running   tcp://192.168.0.2:222222     " ));

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "tcp://192.168.0.2:22222"));
    }

    @Test
    public void dockerUriTcpShouldBeReplacedToHttpInCaseOfSingleHost() {
        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "tcp://192.168.0.2:22222");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "tcp://192.168.0.2:22222"));
    }

    @Test
    public void dockerUriTcpShouldBeReplacedToHttpsInCaseOfDockerHostTagPresent() {
        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "tcp://dockerHost:22222");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
        when(commandLineExecutor.execCommand("boot2docker", "ip")).thenReturn("192.168.0.2");

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "tcp://192.168.0.2:22222"));
    }

    @Test
    public void dockerUriTcpShouldBeReplacedToHttpsInCaseOfCertPathPresent() {
        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "tcp://dockerHost:22222");
        config.put(CubeDockerConfiguration.CERT_PATH, "~/.ssh");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
        when(commandLineExecutor.execCommand("boot2docker", "ip")).thenReturn("192.168.0.2");

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "tcp://192.168.0.2:22222"));
    }

    @Test
    public void dockerUriConfigurationParameterShouldTakePrecedenceOverSystemEnv() {
        String originalVar = System.getProperty(CubeDockerConfigurator.DOCKER_HOST);
        try {
            System.setProperty(CubeDockerConfigurator.DOCKER_HOST, "tcp://127.0.0.1:22222");

            Map<String, String> config = new HashMap<>();
            config.put(CubeDockerConfiguration.DOCKER_URI, "https://dockerHost:22222");

            when(extensionDef.getExtensionProperties()).thenReturn(config);
            when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);

            when(extensionDef.getExtensionProperties()).thenReturn(config);
            when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
            when(commandLineExecutor.execCommand("boot2docker", "ip")).thenReturn("192.168.0.1");

            fire(new CubeConfiguration());
            assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "https://192.168.0.1:22222"));
            assertThat(config, hasEntry(is(CubeDockerConfiguration.CERT_PATH), endsWith(
                    File.separator + ".boot2docker" + File.separator + "certs" + File.separator + "boot2docker-vm")));
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

        PrintStream old = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        CubeConfiguration cubeConfiguration = new CubeConfiguration();
        fire(cubeConfiguration);
        System.out.flush();
        System.setOut(old);
        assertThat(baos.toString(), containsString("CubeDockerConfiguration:"));
    }
}
