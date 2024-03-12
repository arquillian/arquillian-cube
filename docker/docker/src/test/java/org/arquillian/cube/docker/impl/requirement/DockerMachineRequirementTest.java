package org.arquillian.cube.docker.impl.requirement;

import org.arquillian.cube.docker.impl.util.CommandLineExecutor;
import org.arquillian.cube.spi.requirement.UnsatisfiedRequirementException;
import org.arquillian.spacelift.execution.ExecutionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DockerMachineRequirementTest {

    @Mock
    CommandLineExecutor commandLineExecutor;

    @Test(expected = UnsatisfiedRequirementException.class)
    public void estDockerMachineRequirementCheckWhenExecutionExceptionThrown() throws UnsatisfiedRequirementException {
        when(commandLineExecutor.execCommandAsArray(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenThrow(ExecutionException.class);

        DockerMachineRequirement dockerMachineRequirement = new DockerMachineRequirement(commandLineExecutor);
        dockerMachineRequirement.check(createContext("testing"));
    }

    @Test(expected = UnsatisfiedRequirementException.class)
    public void testDockerMachineRequirementCheckNoMatchingNameFound() throws Exception {
        when(commandLineExecutor.execCommandAsArray(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(Collections.emptyList());

        DockerMachineRequirement dockerMachineRequirement = new DockerMachineRequirement(commandLineExecutor);
        dockerMachineRequirement.check(createContext("testing"));
    }

    @Test(expected = UnsatisfiedRequirementException.class)
    public void testDockerMachineRequirementCheckNoMachineFound() throws Exception {
        when(commandLineExecutor.execCommandAsArray(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(Arrays.asList(new String[] {"foo", "bar"}));

        DockerMachineRequirement dockerMachineRequirement = new DockerMachineRequirement(commandLineExecutor);
        dockerMachineRequirement.check(createContext(""));
    }

    @Test(expected = UnsatisfiedRequirementException.class)
    public void testDockerMachineRequirementCheckNoMatchingNameNotMatched() throws Exception {
        when(commandLineExecutor.execCommandAsArray(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(Arrays.asList(new String[] {"my-docker-machine"}));

        DockerMachineRequirement dockerMachineRequirement = new DockerMachineRequirement(commandLineExecutor);
        dockerMachineRequirement.check(createContext("testing"));
    }

    @Test
    public void testDockerMachineRequirementCheckNoMatchingNameMatched() throws Exception {
        when(commandLineExecutor.execCommandAsArray(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(Arrays.asList(new String[] {"testing"}));

        DockerMachineRequirement dockerMachineRequirement = new DockerMachineRequirement(commandLineExecutor);
        dockerMachineRequirement.check(createContext("testing"));
    }

    private RequiresDockerMachine createContext(String machineName) {
        return new RequiresDockerMachine() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return RequiresDockerMachine.class;
            }

            @Override
            public String name() {
                return machineName;
            }
        };
    }
}
