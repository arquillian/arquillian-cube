package org.arquillian.cube.docker.impl.client;

import static org.junit.Assert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.endsWith;

import org.arquillian.cube.docker.impl.util.Boot2Docker;
import org.arquillian.cube.docker.impl.util.CommandLineExecutor;
import org.arquillian.cube.docker.impl.util.DockerMachine;
import org.arquillian.cube.spi.CubeConfiguration;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.config.descriptor.api.ExtensionDef;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.test.AbstractManagerTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

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

    @Before
    public void setup() {
        bind(ApplicationScoped.class, Boot2Docker.class, new Boot2Docker(commandLineExecutor));
        bind(ApplicationScoped.class, DockerMachine.class, new DockerMachine(commandLineExecutor));
        bind(ApplicationScoped.class, ArquillianDescriptor.class, arquillianDescriptor);
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
        assertThat(config, hasEntry(is(CubeDockerConfiguration.CERT_PATH), endsWith(File.separator + ".boot2docker" + File.separator + "certs" + File.separator + "boot2docker-vm")));
    }

    @Test
    public void shouldUseDockerMachineIfDockerHostIsSetOnServerURIAndMachineNameIsSet() {
        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "https://dockerHost:22222");
        config.put(CubeDockerConfiguration.DOCKER_MACHINE_NAME, "dev");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
        when(commandLineExecutor.execCommand("docker-machine", "ip", "dev")).thenReturn("192.168.0.2");

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "https://192.168.0.2:22222"));
        assertThat(config, hasEntry(is(CubeDockerConfiguration.CERT_PATH), endsWith(File.separator + ".docker" + File.separator + "machine" + File.separator + "machines" + File.separator + config.get(CubeDockerConfiguration.DOCKER_MACHINE_NAME))));

    }

    @Test
    public void shouldUseHostEnvIfDockerHostIsSetOnServerURIAndSystemEnvironmentVarIsSet() {
        String originalVar = System.getProperty(CubeDockerConfigurator.DOCKER_HOST);
        System.setProperty(CubeDockerConfigurator.DOCKER_HOST, "tcp://127.0.0.1:22222");

        Map<String, String> config = new HashMap<>();

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "tcp://127.0.0.1:22222"));
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_SERVER_IP, "127.0.0.1"));

        if(originalVar != null) {
            System.setProperty(CubeDockerConfigurator.DOCKER_HOST, originalVar);
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

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "https://192.168.0.2:22222"));
    }

    @Test
    public void dockerUriTcpShouldBeReplacedToHttpInCaseOfSingleHost() {
        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "tcp://192.168.0.2:22222");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "http://192.168.0.2:22222"));
    }

    @Test
    public void dockerUriTcpShouldBeReplacedToHttpsInCaseOfDockerHostTagPresent() {
        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "tcp://dockerHost:22222");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
        when(commandLineExecutor.execCommand("boot2docker", "ip")).thenReturn("192.168.0.2");

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "https://192.168.0.2:22222"));
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
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "https://192.168.0.2:22222"));
    }

    @Test
    public void dockerUriConfigurationParameterShouldTakePrecedenceOverSystemEnv() {
        String originalVar = System.getProperty(CubeDockerConfigurator.DOCKER_HOST);
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
        assertThat(config, hasEntry(is(CubeDockerConfiguration.CERT_PATH), endsWith(File.separator + ".boot2docker" + File.separator + "certs" + File.separator + "boot2docker-vm")));
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_SERVER_IP, "192.168.0.1"));

        if(originalVar != null) {
            System.setProperty(CubeDockerConfigurator.DOCKER_HOST, originalVar);
        }
    }

}
