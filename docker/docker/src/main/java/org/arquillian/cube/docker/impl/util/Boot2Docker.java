package org.arquillian.cube.docker.impl.util;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Boot2Docker {

    public static final String BOOT2DOCKER_TAG = "boot2docker";
    private static final String BOOT2DOCKER_EXEC = "boot2docker";

    private static final Pattern IP_PATTERN = Pattern.compile("(?:\\d{1,3}\\.){3}\\d{1,3}");
    private static final Logger log = Logger.getLogger(Boot2Docker.class.getName());

    private CommandLineExecutor commandLineExecutor;
    private String cachedIp = null;

    public Boot2Docker(CommandLineExecutor commandLineExecutor) {
        this.commandLineExecutor = commandLineExecutor;
    }

    public String ip(String boot2DockerPath, boolean force) {
        if(cachedIp == null || force) {
            cachedIp = getIp(boot2DockerPath);
        }
        return cachedIp;
    }

    private String getIp(String boot2DockerPath) {
        String output = commandLineExecutor.execCommand(createBoot2DockerCommand(boot2DockerPath), "ip");
        Matcher m = IP_PATTERN.matcher(output);
        if(m.find()) {
            String ip = m.group();
            return ip;
        } else {
            String errorMessage = String.format("Boot2Docker command does not return a valid ip. It returned %s.", output);
            log.log(Level.SEVERE, errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private String createBoot2DockerCommand(String boot2DockerPath) {
        return boot2DockerPath == null ? BOOT2DOCKER_EXEC : boot2DockerPath;
    }

}
