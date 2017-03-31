package org.arquillian.cube.docker.impl.util;

import java.util.regex.Pattern;

public class Boot2Docker extends AbstractCliInternetAddressResolver {

    private static final String BOOT2DOCKER_EXEC = "boot2docker";

    private static final Pattern IP_PATTERN = Pattern.compile("(?:\\d{1,3}\\.){3}\\d{1,3}");

    public Boot2Docker(CommandLineExecutor commandLineExecutor) {
        super(commandLineExecutor);
    }

    @Override
    protected String[] getCommandArguments(String cliPathExec) {
        return new String[] {createBoot2DockerCommand(cliPathExec), "ip"};
    }

    @Override
    protected Pattern getIpPattern() {
        return IP_PATTERN;
    }

    private String createBoot2DockerCommand(String boot2DockerPath) {
        return boot2DockerPath == null ? BOOT2DOCKER_EXEC : boot2DockerPath;
    }
}
