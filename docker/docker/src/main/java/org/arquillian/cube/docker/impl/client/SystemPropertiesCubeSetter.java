package org.arquillian.cube.docker.impl.client;

import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.model.DockerCube;
import org.arquillian.cube.spi.Binding;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.event.lifecycle.AfterDestroy;
import org.arquillian.cube.spi.event.lifecycle.AfterStart;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;

import java.util.Iterator;
import java.util.Map;

public class SystemPropertiesCubeSetter {

    private static final String PREFIX = "arq.cube.docker";

    public void createDockerHostProperty(@Observes BeforeSuite beforeSuite, DockerClientExecutor dockerClientExecutor) {
        System.setProperty(String.format("%s.host", PREFIX), dockerClientExecutor.getDockerServerIp());
    }

    public void removeDockerHostProperty(@Observes AfterSuite afterSuite) {
        System.clearProperty(String.format("%s.host", PREFIX));
    }

    public void createCubeSystemProperties(@Observes AfterStart afterStart, CubeRegistry cubeRegistry) {
        String cubeId = afterStart.getCubeId();

        final DockerCube cube = cubeRegistry.getCube(cubeId, DockerCube.class);

        final Binding bindings = cube.bindings();
        final String cubePrefix = String.format("%s.%s", PREFIX, cubeId);

        final String ip = bindings.getIP();
        final String internalIP = bindings.getInternalIP();
        System.setProperty(String.format("%s.ip", cubePrefix), null != ip ? ip : "");
        System.setProperty(String.format("%s.internal.ip", cubePrefix), null != internalIP ? internalIP : "");

        for (Binding.PortBinding portBinding : bindings.getPortBindings()) {
            final int exposedPort = portBinding.getExposedPort();
            final Integer boundPort = portBinding.getBindingPort();
            System.setProperty(String.format("%s.port.%d", cubePrefix, exposedPort), boundPort.toString());
        }
    }


    public void removeCubeSystemProperties(@Observes AfterDestroy afterDestroy) {
        final Iterator<Map.Entry<Object, Object>> propertiesIterator = System.getProperties()
            .entrySet().iterator();

        String cubePrefix = String.format("%s.%s", PREFIX, afterDestroy.getCubeId());

        while (propertiesIterator.hasNext()) {
            if (propertiesIterator.next().getKey().toString().startsWith(cubePrefix)) {
                propertiesIterator.remove();
            }
        }
    }

}
