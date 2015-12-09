package org.arquillian.cube.docker.impl.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DockerMachineTest {


    @Mock
    private CommandLineExecutor executor;

    @Test
    public void shouldListDockerMachines() {
        when(executor.execCommandAsArray("docker-machine", "ls")).thenReturn(new String[]{
                "NAME   ACTIVE   DRIVER       STATE     URL                         SWARM",
                "dev    *        virtualbox   Running   tcp://192.168.99.100:2376     ",
                "qa    *        virtualbox   Running   tcp://192.168.99.101:2376     swarm-master"
        });

        DockerMachine dockerMachine = new DockerMachine(executor);
        final Set<Machine> list = dockerMachine.list();
        assertThat(list, hasSize(2));
        final Machine[] machines = list.toArray(new Machine[2]);
        assertThat(machines[0].getName(), is("qa"));
        assertThat(machines[0].getState(), is("Running"));
        assertThat(machines[0].getSwarm(), is("swarm-master"));

        assertThat(machines[1].getName(), is("dev"));
        assertThat(machines[1].getState(), is("Running"));
        assertThat(machines[1].getSwarm(), is(""));

    }

    @Test
    public void shouldListWithFilterDockerMachines() {
        when(executor.execCommandAsArray("docker-machine", "ls", "--filter", "state=Running")).thenReturn(new String[]{
                "NAME   ACTIVE   DRIVER       STATE     URL                         SWARM",
                "dev    *        virtualbox   Running   tcp://192.168.99.100:2376     ",
                "qa    *        virtualbox   Running   tcp://192.168.99.101:2376     swarm-master"
        });

        DockerMachine dockerMachine = new DockerMachine(executor);
        final Set<Machine> list = dockerMachine.list("state", "Running");
        assertThat(list, hasSize(2));
        final Machine[] machines = list.toArray(new Machine[2]);
        assertThat(machines[0].getName(), is("qa"));
        assertThat(machines[0].getState(), is("Running"));
        assertThat(machines[0].getSwarm(), is("swarm-master"));

        assertThat(machines[1].getName(), is("dev"));
        assertThat(machines[1].getState(), is("Running"));
        assertThat(machines[1].getSwarm(), is(""));

    }

}
