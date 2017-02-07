package com.github.dockerjava.assertions;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.util.Objects;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Eddú Meléndez
 */
public class ContainerAssert extends AbstractAssert<ContainerAssert, InspectContainerResponse> {

   public ContainerAssert(InspectContainerResponse actual) {
      super(actual, ContainerAssert.class);
   }

   public ContainerAssert hasExposedPorts(String... ports) {
      isNotNull();

      List<ExposedPort> exposedPorts = new ArrayList<ExposedPort>();
      for (String port : ports) {
         exposedPorts.add(ExposedPort.parse(port));
      }

      ExposedPort[] myports = new ExposedPort[exposedPorts.size()];
      exposedPorts.toArray(myports);

      assertThat(getContainerConfig().getExposedPorts())
              .overridingErrorMessage("%nExpecting:%n <%s>%nto contain:%n <%s>", getContainerConfig().getExposedPorts(), Arrays.asList(ports))
              .contains(myports);

      return this;
   }

   public ContainerAssert hasProcessRunning(String process) {
      isNotNull();

      InspectContainerResponse.ContainerState state = getContainerState();
      ContainerStateAssert stateAssert = new ContainerStateAssert(state);
      stateAssert.isNotNull();

      this.isRunning();

      final String actualProcessName = getProcessName(state.getPid());

      assertThat(actualProcessName).isNotNull();
      assertThat(actualProcessName).isNotEmpty();
      assertThat(actualProcessName).isEqualToIgnoringCase(process);

      return this;
   }

   public ContainerAssert hasBindPorts(String... ports) {
      isNotNull();

      List<ExposedPort> exposedPorts = new ArrayList<ExposedPort>();
      for (String port : ports) {
         exposedPorts.add(ExposedPort.parse(port));
      }

      ExposedPort[] myports = new ExposedPort[exposedPorts.size()];
      exposedPorts.toArray(myports);

      assertThat(getHostConfig().getPortBindings().getBindings())
              .overridingErrorMessage("%nExpecting:%n <%s>%nto contain:%n <%s>", getHostConfig().getPortBindings().getBindings().keySet(), Arrays.asList(ports))
              .containsKeys(myports);

      return this;
   }

   public ContainerAssert hasName(String name) {
      isNotNull();

      if (!Objects.areEqual(this.actual.getName(), name)) {
         failWithMessage("Expected container's name to be %s but was %s", name, this.actual.getName());
      }

      return this;
   }

   public ContainerAssert hasVolumes(String... volumes) {
      isNotNull();

      assertThat(getContainerConfig().getVolumes())
              .overridingErrorMessage("%nExpecting:%n <%s>%nto contain:%n <%s>", getContainerConfig().getVolumes().keySet(), Arrays.asList(volumes))
              .containsOnlyKeys(volumes);

      return this;
   }

   public ContainerAssert hasVolumes(int size) {
      isNotNull();

      assertThat(getContainerConfig().getVolumes().size())
              .overridingErrorMessage("Expected container's volumes to be %s but was %s", size, getContainerConfig().getVolumes().size())
              .isEqualTo(size);

      return this;
   }

   public ContainerAssert hasStatus(String status) {
      ContainerStateAssert stateAssert = new ContainerStateAssert(getContainerState());
      stateAssert.isNotNull();

      if (!Objects.areEqual(getContainerState().getStatus(), status)) {
         failWithMessage("Expected container's status to be %s but was %s", status, getContainerState().getStatus());
      }

      return this;
   }

   public ContainerAssert isRunning() {
      ContainerStateAssert stateAssert = new ContainerStateAssert(getContainerState());
      stateAssert.isNotNull();

      if (!getContainerState().getRunning()) {
         failWithMessage("Expected container's state running to be %s but was %s", true, false);
      }

      return this;
   }

   public ContainerAssert isPaused() {
      isNotNull();

      ContainerStateAssert stateAssert = new ContainerStateAssert(getContainerState());
      stateAssert.isNotNull();

      if (!getContainerState().getPaused()) {
         failWithMessage("Expected container's state paused to be %s but was %s", true, false);
      }

      return this;
   }

   public ContainerAssert isRestarting() {
      isNotNull();

      if (getContainerState() != null && !getContainerState().getRestarting()) {
         failWithMessage("Expected container's state restarting to be %s but was %s", true, false);
      }

      return this;
   }

   public ContainerAssert hasImage(String name) {
      isNotNull();

      if (getContainerConfig() != null && !Objects.areEqual(getContainerConfig().getImage(), name)) {
         failWithMessage("Expected container's image name to be %s but was %s", name, this.actual.getConfig().getImage());
      }

      return this;
   }

   public ContainerAssert hasNetworkMode(String networkMode) {
      isNotNull();

      if (getHostConfig() != null && !Objects.areEqual(getHostConfig().getNetworkMode(), networkMode)) {
         failWithMessage("Expected container's networkMode to be %s but was %s", networkMode, getHostConfig().getNetworkMode());
      }

      return this;
   }

   public ContainerAssert hasMountSize(int size) {
      isNotNull();

      if (this.actual.getMounts().size() != size) {
         failWithMessage("Expected container's mounts size to be %s but was %s", size, this.actual.getMounts().size());
      }

      return this;
   }

   private InspectContainerResponse.ContainerState getContainerState() {
      return this.actual.getState();
   }

   private HostConfig getHostConfig() {
      return this.actual.getHostConfig();
   }

   private ContainerConfig getContainerConfig() {
      return this.actual.getConfig();
   }

    private static String getProcessName(int processId) {

        final String command = "ps -p " + processId + " -o comm=";
        String output = "";
        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));

            output = reader.lines().collect(Collectors.joining());

        } catch (Exception e) {
            throw new IllegalStateException("Unable to execute command" + command);
        }

        return output;
    }
}
