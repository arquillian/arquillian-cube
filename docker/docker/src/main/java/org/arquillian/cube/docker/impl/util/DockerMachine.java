package org.arquillian.cube.docker.impl.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class DockerMachine extends AbstractCliInternetAddressResolver {

    private static final String DOCKER_MACHINE_EXEC = "docker-machine";

    private static final Pattern IP_PATTERN = Pattern.compile("(?:\\d{1,3}\\.){3}\\d{1,3}");

    private String machineName;

    public DockerMachine(CommandLineExecutor commandLineExecutor) {
        super(commandLineExecutor);
    }

    @Override
    protected String[] getCommandArguments(String cliPathExec) {
        if(machineName == null) {
            throw new IllegalArgumentException("Machine Name cannot be null");
        }

        return new String[]{createDockerMachineCommand(cliPathExec), "ip", machineName};
    }

    @Override
    protected Pattern getIpPattern() {
        return IP_PATTERN;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    /**
     * Executes docker-machine ls command
     * @param cliPathExec location of docker-machine or null if it is on PATH.
     * @return set of machines
     */
    public Set<Machine> list(String cliPathExec) {

        Set<Machine> machines = new HashSet<>();
        String[] output = commandLineExecutor.execCommandAsArray(createDockerMachineCommand(cliPathExec), "ls");

        // skips header
        for (String fields : Arrays.copyOfRange(output, 1, output.length)) {
            machines.add(parse(fields));
        }

        return machines;
    }

    /**
     * Executes docker-machine ls command
     * @return set of machines
     */
    public Set<Machine> list() {
        return this.list(null);
    }

    /**
     * Executes docker-machine ls --filter field=value command
     * @param cliPathExec location of docker-machine or null if it is on PATH.
     * @param field to use in condition
     * @param value value that the field shoudl have
     * @return set of machines
     */
    public Set<Machine> list(String cliPathExec, String field, String value) {
        Set<Machine> machines = new HashSet<>();
        String[] output = commandLineExecutor.execCommandAsArray(createDockerMachineCommand(cliPathExec), "ls", "--filter", field + "=" + value);

        // skips header
        for (String fields : Arrays.copyOfRange(output, 1, output.length)) {
            machines.add(parse(fields));
        }

        return machines;
    }

    /**
     * Executes docker-machine ls --filter field=value command
     * @param field to use in condition
     * @param value value that the field shoudl have
     * @return set of machines
     */
    public Set<Machine> list(String field, String value) {
        return this.list(null, field, value);
    }

    private Machine parse(String output) {
        String[] fields = output.split("\\s+");
        return Machine.toMachine(fields);
    }

    private String createDockerMachineCommand(String dockerMachinePath) {
        return dockerMachinePath == null ? DOCKER_MACHINE_EXEC : dockerMachinePath;
    }
}
