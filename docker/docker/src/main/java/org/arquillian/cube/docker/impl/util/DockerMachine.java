package org.arquillian.cube.docker.impl.util;

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

        return new String[]{createDockerMachineCommand(cliPathExec) ,"ip", machineName};
    }

    @Override
    protected Pattern getIpPattern() {
        return IP_PATTERN;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    private String createDockerMachineCommand(String boot2DockerPath) {
        return boot2DockerPath == null ? DOCKER_MACHINE_EXEC : boot2DockerPath;
    }
}
