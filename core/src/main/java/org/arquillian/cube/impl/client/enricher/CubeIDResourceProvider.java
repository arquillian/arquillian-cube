package org.arquillian.cube.impl.client.enricher;

import java.lang.annotation.Annotation;
import org.arquillian.cube.CubeID;
import org.arquillian.cube.impl.util.ContainerUtil;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.test.impl.enricher.resource.OperatesOnDeploymentAwareProvider;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;

public class CubeIDResourceProvider extends OperatesOnDeploymentAwareProvider {

    @Inject
    private Instance<Container> containerInst;

    @Inject
    private Instance<CubeRegistry> cubeRegistryInst;

    @Override
    public boolean canProvide(Class<?> type) {
        return CubeID.class.isAssignableFrom(type);
    }

    @Override
    public Object doLookup(ArquillianResource resource, Annotation... qualifiers) {
        Container container = containerInst.get();
        if (container == null) {
            throw new IllegalStateException("No Container found in context, can't perform CubeID injection");
        }
        CubeRegistry cubeRegistry = cubeRegistryInst.get();
        if (cubeRegistry == null) {
            throw new IllegalStateException("No CubeRegistry found in context, can't perform CubeID injection");
        }
        Cube<?> cube = cubeRegistry.getCube(ContainerUtil.getCubeIDForContainer(container));
        if (cube == null) {
            throw new IllegalStateException(
                String.format("No Cube found mapped to current Container[%s] with CubeID[%s]", container.getName(),
                    ContainerUtil.getCubeIDForContainer(container)));
        }
        return new CubeID(cube.getId());
    }
}
