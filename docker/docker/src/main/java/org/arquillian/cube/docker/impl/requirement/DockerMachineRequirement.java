package org.arquillian.cube.docker.impl.requirement;

import org.arquillian.cube.docker.impl.util.CommandLineExecutor;
import org.arquillian.cube.spi.requirement.Requirement;
import org.arquillian.cube.spi.requirement.UnsatisfiedRequirementException;
import org.arquillian.spacelift.execution.ExecutionException;

public class DockerMachineRequirement implements Requirement<RequiresDockerMachine> {

    private static final String NEWLINE_PATTERN = "\r\n|\r|\n";
    private final CommandLineExecutor commandLineExecutor = new CommandLineExecutor();


    @Override
    public void check(RequiresDockerMachine context) throws UnsatisfiedRequirementException {
        String name = context.name();
        try {
            if (name != null && !name.isEmpty()) {
                String ip = commandLineExecutor.execCommand(new String[]{"docker-machine", "ip", name});
                if (ip == null || ip.isEmpty()) {
                    throw new UnsatisfiedRequirementException("Docker machine with name: ["+name+"] not found!");
                }
            } else {
                int machines = countLines(commandLineExecutor.execCommand(new String[]{"docker-machine", "ls"})) - 1;
                if (machines > 0) {
                    throw new UnsatisfiedRequirementException("No docker machine found!");
                }
            }
        } catch (ExecutionException e) {
            throw new UnsatisfiedRequirementException("Cannot execute docker-machine command.");
        }
    }


    private static int countLines(String s) {
        String[] lines = s.split(NEWLINE_PATTERN);
        return lines.length;
    }
}
