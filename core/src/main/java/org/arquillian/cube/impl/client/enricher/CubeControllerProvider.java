package org.arquillian.cube.impl.client.enricher;

import java.lang.annotation.Annotation;
import org.arquillian.cube.CubeController;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 */
public class CubeControllerProvider implements ResourceProvider {

    @Inject
    private Instance<CubeController> cubeController;

    @Override
    public boolean canProvide(Class<?> type) {
        return CubeController.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource arg0, Annotation... arg1) {
        CubeController cubeController = this.cubeController.get();

        if (cubeController == null) {
            throw new IllegalStateException("CubeController was not found.");
        }

        return cubeController;
    }
}
