package org.arquillian.cube.impl.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.arquillian.cube.impl.docker.DockerClientExecutor;
import org.arquillian.cube.impl.util.AutoStartOrderUtil;
import org.arquillian.cube.spi.event.CreateCube;
import org.arquillian.cube.spi.event.CubeControlEvent;
import org.arquillian.cube.spi.event.DestroyCube;
import org.arquillian.cube.spi.event.PreRunningCube;
import org.arquillian.cube.spi.event.StartCube;
import org.arquillian.cube.spi.event.StopCube;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.api.threading.ExecutorService;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;

public class CubeSuiteLifecycleController {

    @Inject
    private Event<CubeControlEvent> controlEvent;

    @Inject
    private Instance<DockerClientExecutor> dockerClientExecutor;

    @Inject
    private Instance<ExecutorService> executorServiceInst;

    public void startAutoContainers(@Observes(precedence = 100) BeforeSuite event, final CubeConfiguration configuration) {
        List<String[]> autoStartSteps = AutoStartOrderUtil.getAutoStartOrder(configuration);
        startAllSteps(autoStartSteps, configuration.getConnectionMode());
    }

    public void stopAutoContainers(@Observes(precedence = -100) AfterSuite event, CubeConfiguration configuration) {
        List<String[]> autoStopSteps = AutoStartOrderUtil.getAutoStopOrder(configuration);
        stopAllSteps(autoStopSteps);
    }

    private void startAllSteps(List<String[]> autoStartSteps, ConnectionMode connectionMode) {
        for(final String[] cubeIds : autoStartSteps) {
            Map<String, Future<RuntimeException>> stepStatus = new HashMap<>();

            // Start
            for(final String cubeId : cubeIds) {
                Future<RuntimeException> result = executorServiceInst.get().submit(new StartCubes(cubeId, connectionMode));
                stepStatus.put(cubeId, result);
            }

            waitForCompletion(stepStatus, "Could not auto start container");
        }
    }

    private void stopAllSteps(List<String[]> autoStopSteps) {
        for(final String[] cubeIds : autoStopSteps) {
            Map<String, Future<RuntimeException>> stepStatus = new HashMap<>();

            // Start
            for(final String cubeId : cubeIds) {
                Future<RuntimeException> result = executorServiceInst.get().submit(new StopCubes(cubeId));
                stepStatus.put(cubeId, result);
            }

            // wait
            waitForCompletion(stepStatus, "Could not auto stop container");
        }
    }

    private void waitForCompletion(Map<String, Future<RuntimeException>> stepStatus, String message) {
        for(final Map.Entry<String, Future<RuntimeException>> result: stepStatus.entrySet()) {
            try {
                RuntimeException e = result.getValue().get();
                if(e != null) {
                    throw e;
                }
            } catch (Exception e) {
                throw new RuntimeException(message + " " + result.getKey(), e);
            }
        }
    }

    private boolean isCubeRunning(String cube) {
        //TODO should we create an adapter class so we don't expose client classes in this part?
        List<com.github.dockerjava.api.model.Container> runningContainers = dockerClientExecutor.get().listRunningContainers();
        for (com.github.dockerjava.api.model.Container container : runningContainers) {
            for (String name : container.getNames()) {
                if(name.startsWith("/")) name = name.substring(1); //Names array adds an slash to the docker name container.
                if(name.equals(cube)) { //cube id is the container name in docker0 Id in docker is the hash that identifies it.
                    return true;
                }
            }
        }
        return false;
    }

    private final class StartCubes implements Callable<RuntimeException> {
        private final ConnectionMode connectionMode;
        private final String cubeId;

        private StartCubes(String cubeId, ConnectionMode connectionMode) {
            this.cubeId = cubeId;
            this.connectionMode = connectionMode;
        }

        @Override
        public RuntimeException call() throws Exception {
            try {
                if(connectionMode.isAllowReconnect() && isCubeRunning(cubeId)) {
                    controlEvent.fire(new PreRunningCube(cubeId));
                    return null;
                }
                controlEvent.fire(new CreateCube(cubeId));
                controlEvent.fire(new StartCube(cubeId));

                if(connectionMode.isAllowReconnect() && !connectionMode.isStoppable()) {
                 // If we allow reconnections and containers are none stoppable which means that they will be able to be
                 // reused in next executions then at this point we can assume that the container is a prerunning container.
                 controlEvent.fire(new PreRunningCube(cubeId));
                }
            } catch(RuntimeException e) {
                return e;
            }
            return null;
        }
    }

    private final class StopCubes implements Callable<RuntimeException> {
        private final String cubeId;

        private StopCubes(String cubeId) {
            this.cubeId = cubeId;
        }

        @Override
        public RuntimeException call() throws Exception {
            try {
                controlEvent.fire(new StopCube(cubeId));
                controlEvent.fire(new DestroyCube(cubeId));
            } catch(RuntimeException e) {
                return e;
            }
            return null;
        }
    }
}
