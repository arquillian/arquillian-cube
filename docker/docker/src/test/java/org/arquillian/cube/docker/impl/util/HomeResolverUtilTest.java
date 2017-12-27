package org.arquillian.cube.docker.impl.util;

import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@Category({RequiresDocker.class, RequiresDockerMachine.class})
public class HomeResolverUtilTest {

    private String originalHome = System.getProperty("user.home");

    @After
    public void recoverHomeDirectory() {
        System.setProperty("user.home", originalHome);
    }

    @Test
    public void shouldResolveTildeCharacterToHome() {
        System.setProperty("user.home", "/home/arquillian");
        String resolvedPath = HomeResolverUtil.resolveHomeDirectoryChar("~/certs");
        assertThat(resolvedPath, is("/home/arquillian/certs"));
    }
}
