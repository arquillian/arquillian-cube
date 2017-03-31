package org.arquillian.cube.openshift.ftest.test;

import java.net.URL;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.impl.base.io.IOUtil;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

//@BelongsTo(IntegrationSuite.class)
@RunWith(Arquillian.class)
@RunAsClient
public class IndexTestCase {

    @Test @InSequence(1)
    public void shouldBeAbleToInjectURL(@ArquillianResource URL base) throws Exception {
        System.out.println(base);
        Assert.assertNotNull(base);

        IOUtil.copy(base.openStream(), System.out);
    }

    @Test @InSequence(2)
    public void debug() {
        System.out.println("");
    }
}
