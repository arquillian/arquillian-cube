package org.arquillian.cube.docker.impl.util;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractCliInternetAddressResolver {

    public static final String DOCKERHOST_TAG = "dockerHost";
    private static final Logger log = Logger.getLogger(AbstractCliInternetAddressResolver.class.getName());

    private CommandLineExecutor commandLineExecutor;
    private String cachedIp = null;

    public AbstractCliInternetAddressResolver(final CommandLineExecutor commandLineExecutor) {
        this.commandLineExecutor = commandLineExecutor;
    }

    public String ip(String cliPathExec, boolean force) {
        if(cachedIp == null || force) {
            cachedIp = getIp(cliPathExec);
        }
        return cachedIp;
    }

    private String getIp(String cliPathExec) {
        String output = commandLineExecutor.execCommand(getCommandArguments(cliPathExec));
        Matcher m = getIpPattern().matcher(output);
        if(m.find()) {
            String ip = m.group();
            return ip;
        } else {
            String errorMessage = String.format("Cli Internet address resolver executed %s command and does not return a valid ip. It returned %s.",
                    Arrays.toString(getCommandArguments(cliPathExec)), output);
            log.log(Level.SEVERE, errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    protected abstract String[] getCommandArguments(String cliPathExec);
    protected abstract Pattern getIpPattern();
}
