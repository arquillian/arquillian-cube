package org.arquillian.cube.docker.impl.requirement;

import java.util.List;

import org.arquillian.cube.docker.impl.util.CommandLineExecutor;
import org.arquillian.cube.spi.requirement.Requirement;
import org.arquillian.cube.spi.requirement.UnsatisfiedRequirementException;
import org.arquillian.spacelift.execution.ExecutionException;

public class DockerMachineRequirement implements Requirement<RequiresDockerMachine> {

    private final CommandLineExecutor commandLineExecutor = new CommandLineExecutor();


    @Override
    public void check(RequiresDockerMachine context) throws UnsatisfiedRequirementException {
        String name = context.name();
        try {
            if (name != null && !name.isEmpty()) {
                List<String> machines = commandLineExecutor.execCommandAsArray("docker-machine", "ls", "--filter", "name=" + name, "--format", "{{.Name}}");
                if (!machines.contains(name)) {
                    throw new UnsatisfiedRequirementException("Docker machine with name: ["+name+"] not found!");
                }
            } else {
                List<String> machines = commandLineExecutor.execCommandAsArray("docker-machine", "ls", "--format", "{{.Name}}");
                if (machines.size() > 0) {
                    throw new UnsatisfiedRequirementException("No docker machine found!");
                }
            }
        } catch (ExecutionException e) {
            throw new UnsatisfiedRequirementException("Cannot execute docker-machine command.");
        }
    }

}
