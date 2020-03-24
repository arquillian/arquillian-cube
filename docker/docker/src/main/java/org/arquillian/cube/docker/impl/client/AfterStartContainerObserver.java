package org.arquillian.cube.docker.impl.client;

import org.arquillian.cube.docker.impl.afterStart.AfterStartActionFactory;
import org.arquillian.cube.docker.impl.client.config.AfterStart;
import org.arquillian.cube.docker.impl.client.config.Copy;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.CustomAfterStartAction;
import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.docker.impl.model.DockerCube;
import org.arquillian.cube.impl.model.DefaultCubeId;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.jboss.arquillian.core.api.annotation.Observes;

import java.io.File;
import java.util.Collection;

public class AfterStartContainerObserver {

    public void processCommands(@Observes org.arquillian.cube.spi.event.lifecycle.AfterStart afterStart,
        CubeRegistry cubeRegistry,
        DockerClientExecutor dockerClientExecutor) {

        Cube<CubeContainer> cube = cubeRegistry.getCube(afterStart.getCubeId(), DockerCube.class);
        CubeContainer configuration = cube.configuration();

        if (configuration.getAfterStart() != null) {
            Collection<AfterStart> afterStartConfiguration = configuration.getAfterStart();

            for (AfterStart map : afterStartConfiguration) {
                if (map.getCopy() != null) {
                    Copy copyConfiguration = map.getCopy();
                    executeCopyAction(dockerClientExecutor, afterStart.getCubeId(), copyConfiguration);
                }
                if (map.getCustomAfterStartAction() != null) {
                    CustomAfterStartAction customAfterStartAction = map.getCustomAfterStartAction();
                    executeCustomAfterStartAction(dockerClientExecutor, afterStart.getCubeId(), customAfterStartAction);
                }
            }
        }
    }

    private void executeCustomAfterStartAction(DockerClientExecutor dockerClientExecutor, String containerId,
        CustomAfterStartAction customAfterStartAction) {
        AfterStartActionFactory.create(dockerClientExecutor, new DefaultCubeId(containerId), customAfterStartAction)
            .doAfterStart();
    }

    private void executeCopyAction(DockerClientExecutor dockerClientExecutor, String containerId,
        Copy configurationParameters) {
        String to;
        String from;
        if (configurationParameters.getTo() != null && configurationParameters.getFrom() != null) {
            to = configurationParameters.getTo();
            from = configurationParameters.getFrom();
        } else {
            throw new IllegalArgumentException(
                String.format("to and from property is mandatory when copying files to container %s.", containerId));
        }

        dockerClientExecutor.copyStreamToContainer(containerId, new File(from), to);
    }
}
