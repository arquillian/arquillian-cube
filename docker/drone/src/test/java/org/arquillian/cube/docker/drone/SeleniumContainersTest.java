package org.arquillian.cube.docker.drone;

import org.arquillian.cube.docker.drone.CubeDroneConfiguration.ContainerNameStrategy;
import org.arquillian.cube.docker.impl.client.config.PortBinding;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SeleniumContainersTest {

    private final static Matcher<PortBinding> SELENIUM_RANDOM_PORT_BINDING_MATCHER =
            new CustomMatcher<PortBinding>("a port binding for Selenium exposed port"){
                @Override
                public boolean matches(Object item){
                    if (item instanceof PortBinding) {
                        PortBinding portBinding = (PortBinding) item;
                        return portBinding.getExposedPort().getExposed() == 4444 
                                && portBinding.getBound() >= 49152 && portBinding.getBound() < 65535;
                    }
                    return false;
                }
            };

    private final static Matcher<String> UUID_CONTAINER_NAME_MATCHER =
            new CustomMatcher<String>("a String matching 'browser|vnc|flv2mp4_UUID'"){
                @Override
                public boolean matches(Object item){
                    if (item instanceof String) {
                        String string = (String) item;
                        return string.matches("(browser|vnc|flv2mp4)_[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
                    }
                    return false;
                }
            };
    
    @Mock
    CubeDroneConfiguration cubeDroneConfiguration;

    @Test
    public void shouldCreateCustomContainerFromImageName() {
        when(cubeDroneConfiguration.isBrowserDockerfileDirectorySet()).thenReturn(false);
        when(cubeDroneConfiguration.isBrowserImageSet()).thenReturn(true);
        when(cubeDroneConfiguration.getBrowserImage()).thenReturn("mycompany/mybrowser:1.0");
        when(cubeDroneConfiguration.getContainerNameStrategy()).thenReturn(ContainerNameStrategy.STATIC);

        final SeleniumContainers firefox = SeleniumContainers.create("firefox", cubeDroneConfiguration);
        assertThat(firefox.getBrowser(), is("firefox"));
        assertThat(firefox.getSeleniumContainer().getImage().toString(), is("mycompany/mybrowser:1.0"));
        assertThat(firefox.getSeleniumContainer().getPortBindings(), hasItem(PortBinding.valueOf("14444->4444")));
    }

    @Test
    public void shouldCreateCustomContainerFromDockerfile() {
        when(cubeDroneConfiguration.isBrowserDockerfileDirectorySet()).thenReturn(true);
        when(cubeDroneConfiguration.isBrowserImageSet()).thenReturn(false);
        when(cubeDroneConfiguration.getBrowserDockerfileLocation()).thenReturn("src/test/resources/browser");
        when(cubeDroneConfiguration.getContainerNameStrategy()).thenReturn(ContainerNameStrategy.STATIC);

        final SeleniumContainers firefox = SeleniumContainers.create("firefox", cubeDroneConfiguration);
        assertThat(firefox.getBrowser(), is("firefox"));
        assertThat(firefox.getSeleniumContainer().getBuildImage().getDockerfileLocation().toString(),
            is("src/test/resources/browser"));
        assertThat(firefox.getSeleniumContainer().getPortBindings(), hasItem(PortBinding.valueOf("14444->4444")));
    }

    @Test
    public void shouldTakePrecedenceDockerfileDirectoryThanImage() {
        when(cubeDroneConfiguration.isBrowserDockerfileDirectorySet()).thenReturn(true);
        when(cubeDroneConfiguration.isBrowserImageSet()).thenReturn(true);
        when(cubeDroneConfiguration.getBrowserDockerfileLocation()).thenReturn("src/test/resources/browser");
        when(cubeDroneConfiguration.getContainerNameStrategy()).thenReturn(ContainerNameStrategy.STATIC);

        final SeleniumContainers firefox = SeleniumContainers.create("firefox", cubeDroneConfiguration);
        assertThat(firefox.getBrowser(), is("firefox"));
        assertThat(firefox.getSeleniumContainer().getBuildImage().getDockerfileLocation().toString(),
            is("src/test/resources/browser"));
        assertThat(firefox.getSeleniumContainer().getImage(), is(nullValue()));
        assertThat(firefox.getSeleniumContainer().getPortBindings(), hasItem(PortBinding.valueOf("14444->4444")));
    }

    @Test
    public void shouldCreateContainerForFirefox() {
        when(cubeDroneConfiguration.isBrowserDockerfileDirectorySet()).thenReturn(false);
        when(cubeDroneConfiguration.isBrowserImageSet()).thenReturn(false);
        when(cubeDroneConfiguration.getContainerNameStrategy()).thenReturn(ContainerNameStrategy.STATIC);

        final SeleniumContainers firefox = SeleniumContainers.create("firefox", cubeDroneConfiguration);
        assertThat(firefox.getBrowser(), is("firefox"));
        assertThat(firefox.getSeleniumContainer().getImage().toString(), is("selenium/standalone-firefox-debug:2.53.0"));
        assertThat(firefox.getSeleniumContainer().getPortBindings(), hasItem(PortBinding.valueOf("14444->4444")));
        assertThat(firefox.getSeleniumContainer().getAwait().getResponseCode(), is(403));
    }

    @Test
    public void shouldCreateContainerForChrome() {

        when(cubeDroneConfiguration.isBrowserDockerfileDirectorySet()).thenReturn(false);
        when(cubeDroneConfiguration.isBrowserImageSet()).thenReturn(false);
        when(cubeDroneConfiguration.getContainerNameStrategy()).thenReturn(ContainerNameStrategy.STATIC);

        final SeleniumContainers firefox = SeleniumContainers.create("chrome", cubeDroneConfiguration);
        assertThat(firefox.getBrowser(), is("chrome"));
        assertThat(firefox.getSeleniumContainer().getImage().toString(), is("selenium/standalone-chrome-debug:2.53.0"));
        assertThat(firefox.getSeleniumContainer().getPortBindings(), hasItem(PortBinding.valueOf("14444->4444")));
        assertThat(firefox.getSeleniumContainer().getAwait().getResponseCode(), is(403));
    }

    @Test
    public void shouldCreateRandomContainerNameAndPort() {
        when(cubeDroneConfiguration.isBrowserDockerfileDirectorySet()).thenReturn(false);
        when(cubeDroneConfiguration.isBrowserImageSet()).thenReturn(false);
        when(cubeDroneConfiguration.getContainerNameStrategy()).thenReturn(ContainerNameStrategy.RANDOM);

        final SeleniumContainers seleniumContainers = SeleniumContainers.create("firefox", cubeDroneConfiguration);
        assertThat(seleniumContainers.getSeleniumContainer().getPortBindings(), hasItem(SELENIUM_RANDOM_PORT_BINDING_MATCHER));
        assertThat(seleniumContainers.getSeleniumContainerName(), both(UUID_CONTAINER_NAME_MATCHER).and(startsWith("browser")));
        assertThat(seleniumContainers.getVncContainerName(), both(UUID_CONTAINER_NAME_MATCHER).and(startsWith("vnc")));
        assertThat(seleniumContainers.getVideoConverterContainerName(), both(UUID_CONTAINER_NAME_MATCHER).and(startsWith("flv2mp4")));
    }

    @Test
    public void shouldCreateStaticPrefixedContainerNameAndPort() {
        when(cubeDroneConfiguration.isBrowserDockerfileDirectorySet()).thenReturn(false);
        when(cubeDroneConfiguration.isBrowserImageSet()).thenReturn(false);
        when(cubeDroneConfiguration.getContainerNameStrategy()).thenReturn(ContainerNameStrategy.STATIC_PREFIX);
        when(cubeDroneConfiguration.getContainerNamePrefix()).thenReturn("test");

        final SeleniumContainers seleniumContainers = SeleniumContainers.create("firefox", cubeDroneConfiguration);
        assertThat(seleniumContainers.getSeleniumContainer().getPortBindings(), hasItem(SELENIUM_RANDOM_PORT_BINDING_MATCHER));
        assertThat(seleniumContainers.getSeleniumContainerName(), is("test_browser"));
        assertThat(seleniumContainers.getVncContainerName(), is("test_vnc"));
        assertThat(seleniumContainers.getVideoConverterContainerName(), is("test_flv2mp4"));
    }

    @Test
    public void shouldCreateStaticContainerNameAndPort() {
        when(cubeDroneConfiguration.isBrowserDockerfileDirectorySet()).thenReturn(false);
        when(cubeDroneConfiguration.isBrowserImageSet()).thenReturn(false);
        when(cubeDroneConfiguration.getContainerNameStrategy()).thenReturn(ContainerNameStrategy.STATIC);

        final SeleniumContainers seleniumContainers = SeleniumContainers.create("firefox", cubeDroneConfiguration);
        assertThat(seleniumContainers.getSeleniumContainer().getPortBindings(), hasItem(PortBinding.valueOf("14444->4444")));
        assertThat(seleniumContainers.getSeleniumContainerName(), is("browser"));
        assertThat(seleniumContainers.getVncContainerName(), is("vnc"));
        assertThat(seleniumContainers.getVideoConverterContainerName(), is("flv2mp4"));
    }
}
