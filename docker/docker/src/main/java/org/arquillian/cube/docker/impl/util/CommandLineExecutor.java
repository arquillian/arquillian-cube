package org.arquillian.cube.docker.impl.util;

import java.io.IOException;
import java.util.Arrays;

public class CommandLineExecutor {

    public String execCommand(String... arguments) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(arguments);
            processBuilder.redirectErrorStream(true);
            Process pwd = processBuilder.start();

            pwd.waitFor();
            String output = IOUtil.asString(pwd.getInputStream());

            // Means an internal error of the application we are launching
            if (pwd.exitValue() > 0) {
                throw new IllegalArgumentException(String.format("Executing command %s has returned error value %s with message \"%s\".", Arrays.toString(arguments), pwd.exitValue(), output));
            }

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