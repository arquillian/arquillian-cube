package org.arquillian.cube.openshift.ftest.test;

import javax.inject.Inject;
import org.arquillian.cube.kubernetes.impl.requirement.RequiresKubernetes;
import org.arquillian.cube.openshift.ftest.EnvPrinter;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

//@BelongsTo(IntegrationSuite.class)
@RunWith(Arquillian.class)
@Category(RequiresOpenshift.class)
public class CDIBeanTestCase {

    @Inject
    private EnvPrinter env;

    @Test
    public void shouldRunInContainer() throws Exception {
        env.print();
    }
}
