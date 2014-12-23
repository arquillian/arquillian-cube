package org.arquillian.cube.impl.client;

import java.util.List;

import org.arquillian.cube.impl.docker.DockerClientExecutor;
import org.arquillian.cube.impl.util.ConfigUtil;
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
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;

public class CubeSuiteLifecycleController {

    @Inject
    private Event<CubeControlEvent> controlEvent;

    @Inject
    private Instance<DockerClientExecutor> dockerClientExecutor;

    public void startAutoContainers(@Observes(precedence = 100) BeforeSuite event, CubeConfiguration configuration) {
        for(String cubeId : configuration.getAutoStartContainers()) {

            if(configuration.shouldAllowToConnectToRunningContainers() && isCubeRunning(cubeId)) {
                controlEvent.fire(new PreRunningCube(cubeId));
            } else {
                controlEvent.fire(new CreateCube(cubeId));
                controlEvent.fire(new StartCube(cubeId));
            }
        }
    }

    public void stopAutoContainers(@Observes(precedence = -100) AfterSuite event, CubeConfiguration configuration) {
        for(String cubeId : ConfigUtil.reverse(configuration.getAutoStartContainers())) {
            controlEvent.fire(new StopCube(cubeId));
            controlEvent.fire(new DestroyCube(cubeId));
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
}
