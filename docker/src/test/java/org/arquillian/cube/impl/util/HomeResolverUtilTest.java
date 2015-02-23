package org.arquillian.cube.impl.util;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;

import org.junit.After;
import org.junit.Test;

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
