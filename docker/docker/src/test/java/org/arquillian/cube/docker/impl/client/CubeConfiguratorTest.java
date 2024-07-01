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
import org.arquillian.cube.docker.impl.util.CommandLineExecutor;
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
        bind(ApplicationScoped.class, ArquillianDescriptor.class, arquillianDescriptor);
        bind(ApplicationScoped.class, Top.class, top);

        when(top.isSpinning()).thenReturn(false);
        
        environmentVariables.clear("DOCKER_HOST", "DOCKER_TLS_VERIFY", "DOCKER_CERT_PATH");
    }

    @Test
    public void shouldChangeServerUriInCaseOfRunningDockerInsideDocker() {
        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "https://dockerHost:22222");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        lenient().when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
        when(top.isSpinning()).thenReturn(true);

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, OperatingSystemFamily.DIND.getServerUri()));
    }


    @Test
    public void tlsVerifyShouldBeTrueInCaseOfHttpsServerUri() {
        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "https://192.168.0.2:22222");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "tcp://192.168.0.2:22222"));
        assertThat(config, hasEntry(CubeDockerConfiguration.TLS_VERIFY, "true"));
    }

    @Test
    public void tlsVerifyShouldBeTrueInCaseOfSetToFalseAndHttpsServerUri() {
        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "https://192.168.0.2:22222");
        config.put(CubeDockerConfiguration.TLS_VERIFY, "false");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);
        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "tcp://192.168.0.2:22222"));
        assertThat(config, hasEntry(CubeDockerConfiguration.TLS_VERIFY, "true"));
    }

    @Test
    public void tlsVerifyShouldBeFalseInCaseOfHttpServerUri() throws Exception {
        Map<String, String> config = new HashMap<>();
        config.put(CubeDockerConfiguration.DOCKER_URI, "http://192.168.0.2:22222");

        when(extensionDef.getExtensionProperties()).thenReturn(config);
        when(arquillianDescriptor.extension("docker")).thenReturn(extensionDef);

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

        fire(new CubeConfiguration());
        assertThat(config, hasEntry(CubeDockerConfiguration.DOCKER_URI, "tcp://192.168.0.2:22222"));
        assertThat(config, hasEntry(CubeDockerConfiguration.TLS_VERIFY, "false"));
        assertThat(config, not(hasKey(CubeDockerConfiguration.CERT_PATH)));
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
