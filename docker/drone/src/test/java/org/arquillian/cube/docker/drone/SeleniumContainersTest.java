package org.arquillian.cube.docker.drone;

import org.arquillian.cube.docker.impl.client.config.PortBinding;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SeleniumContainersTest {

    @Mock
    CubeDroneConfiguration cubeDroneConfiguration;

    @Test
    public void shouldCreateCustomContainerFromImageName() {
        when(cubeDroneConfiguration.isBrowserDockerfileDirectorySet()).thenReturn(false);
        when(cubeDroneConfiguration.isBrowserImageSet()).thenReturn(true);
        when(cubeDroneConfiguration.getBrowserImage()).thenReturn("mycompany/mybrowser:1.0");

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

        final SeleniumContainers firefox = SeleniumContainers.create("firefox", cubeDroneConfiguration);
        assertThat(firefox.getBrowser(), is("firefox"));
        assertThat(firefox.getSeleniumContainer().getImage().toString(), is("selenium/standalone-firefox-debug:2.53.0"));
        assertThat(firefox.getSeleniumContainer().getPortBindings(), hasItem(PortBinding.valueOf("14444->4444")));
    }

    @Test
    public void shouldAwait200ResponseCodeForSelenium3() {
        when(cubeDroneConfiguration.isBrowserDockerfileDirectorySet()).thenReturn(false);
        when(cubeDroneConfiguration.isBrowserImageSet()).thenReturn(true);
        when(cubeDroneConfiguration.getBrowserImage()).thenReturn("selenium/standalone-firefox-debug:3.4.0");

        final SeleniumContainers firefox = SeleniumContainers.create("firefox", cubeDroneConfiguration);
        assertThat(firefox.getSeleniumContainer().getImage().toString(), is("selenium/standalone-firefox-debug:3.4.0"));
        assertThat(firefox.getSeleniumContainer().getAwait().getResponseCode(), is(200));
    }

    @Test
    public void shouldAwait200ResponseCodeForLatestSelenium() {
        when(cubeDroneConfiguration.isBrowserDockerfileDirectorySet()).thenReturn(false);
        when(cubeDroneConfiguration.isBrowserImageSet()).thenReturn(true);
        when(cubeDroneConfiguration.getBrowserImage()).thenReturn("selenium/standalone-firefox-debug:latest");

        final SeleniumContainers firefox = SeleniumContainers.create("firefox", cubeDroneConfiguration);
        assertThat(firefox.getBrowser(), is("firefox"));
        assertThat(firefox.getSeleniumContainer().getImage().toString(), is("selenium/standalone-firefox-debug:latest"));
        assertThat(firefox.getSeleniumContainer().getAwait().getResponseCode(), is(200));
    }

    @Test
    public void shouldAwait403ResponseCodeForSelenium2() {
        when(cubeDroneConfiguration.isBrowserDockerfileDirectorySet()).thenReturn(false);
        when(cubeDroneConfiguration.isBrowserImageSet()).thenReturn(true);
        when(cubeDroneConfiguration.getBrowserImage()).thenReturn("selenium/standalone-firefox-debug:2.53.0");

        final SeleniumContainers firefox = SeleniumContainers.create("firefox", cubeDroneConfiguration);
        assertThat(firefox.getBrowser(), is("firefox"));
        assertThat(firefox.getSeleniumContainer().getImage().toString(), is("selenium/standalone-firefox-debug:2.53.0"));
        assertThat(firefox.getSeleniumContainer().getAwait().getResponseCode(), is(403));
    }

    @Test
    public void shouldCreateContainerForChrome() {

        when(cubeDroneConfiguration.isBrowserDockerfileDirectorySet()).thenReturn(false);
        when(cubeDroneConfiguration.isBrowserImageSet()).thenReturn(false);

        final SeleniumContainers firefox = SeleniumContainers.create("chrome", cubeDroneConfiguration);
        assertThat(firefox.getBrowser(), is("chrome"));
        assertThat(firefox.getSeleniumContainer().getImage().toString(), is("selenium/standalone-chrome-debug:2.53.0"));
        assertThat(firefox.getSeleniumContainer().getPortBindings(), hasItem(PortBinding.valueOf("14444->4444")));
    }
}
