package org.arquillian.cube.docker.impl.util;

import java.io.IOException;

public class CommandLineExecutor {

    public String execCommand(String... arguments) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(arguments);
            processBuilder.redirectErrorStream(true);
            Process pwd = processBuilder.start();

            pwd.waitFor();
            String output = IOUtil.asString(pwd.getInputStream());
            return output;
        } catch (InterruptedException | IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public String[] execCommandAsArray(String... arguments) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(arguments);
            processBuilder.redirectErrorStream(true);
            Process pwd = processBuilder.start();

            pwd.waitFor();
            String[] output = IOUtil.asArrayString(pwd.getInputStream());
            return output;
        } catch (InterruptedException | IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}