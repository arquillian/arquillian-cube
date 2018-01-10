package org.arquillian.cube.kubernetes.impl;

import java.io.IOException;
import org.arquillian.cube.kubernetes.api.DependencyResolver;
import org.arquillian.cube.kubernetes.api.Session;
import org.arquillian.cube.kubernetes.impl.log.AnsiLogger;
import org.arquillian.cube.kubernetes.impl.resolve.ShrinkwrapResolver;
import org.junit.Assert;
import org.junit.Test;

public class DependencyResolverTest {

    @Test
    public void testResolutionOfPomWithNoDeps() throws IOException {
        Session session = new DefaultSession("test-session", "test-session-123", new AnsiLogger());
        DependencyResolver resolver =
            new ShrinkwrapResolver(DependencyResolver.class.getResource("/test-pom.xml").getFile(), false);
        Assert.assertTrue(resolver.resolve(session).isEmpty());
    }
}
