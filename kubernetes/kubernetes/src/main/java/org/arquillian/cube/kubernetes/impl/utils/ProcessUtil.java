/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.arquillian.cube.kubernetes.impl.utils;

import org.apache.commons.io.FileUtils;
import org.arquillian.cube.impl.util.Strings;
import org.arquillian.cube.kubernetes.api.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;


/**
 * A helper class for running external processes
 *
 * This class is a cut down version taken from the [Fabric8 Maven Plugin](https://github.com/fabric8io/fabric8-maven-plugin/blob/master/core/src/main/java/io/fabric8/maven/core/util/ProcessUtil.java)
 */
public class ProcessUtil {

    public static int runCommand(final Logger log, URL scriptUrl) throws IOException {
        File scriptFile = File.createTempFile("arquillian-cube-script", getSuffix());
        FileUtils.copyURLToFile(scriptUrl, scriptFile);
        scriptFile.setExecutable(true, false);
        return runCommand(log, getCommand(), Arrays.asList(new String[]{scriptFile.getAbsolutePath()}), true);
    }

    public static int runCommand(final Logger log, String command, List<String> args, boolean withShutdownHook) throws IOException {
        String[] commandWithArgs = prepareCommandArray(command, args);
        Process process = Runtime.getRuntime().exec(commandWithArgs);
        if (withShutdownHook) {
            addShutdownHook(log, process, command);
        }
        List<Thread> threads = startLoggingThreads(process, log, command + " " + Strings.join(args, " "));
        try {
            int answer = process.waitFor();
            joinThreads(threads, log);
            return answer;
        } catch (InterruptedException e) {
            return process.exitValue();
        }
    }

    private static void joinThreads(List<Thread> threads, Logger log) {
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                log.warn("Caught " + e.getMessage());
            }
        }
    }


    // ==========================================================================================================

    private static void addShutdownHook(final Logger log, final Process process, final String command) {
        Runtime.getRuntime().addShutdownHook(new Thread(command) {
            @Override
            public void run() {
                if (process != null) {
                    log.info("Terminating process [" + command + "]");
                    try {
                        process.destroy();
                    } catch (Exception e) {
                        log.error("Failed to terminate process [" + command + "]");
                    }
                }
            }
        });
    }

    private static String[] prepareCommandArray(String command, List<String> args) {
        List<String> nCmd = Strings.splitAndTrimAsList(command, " ");
        List<String> nArgs = args != null ? args : new ArrayList<String>();
        String[] commandWithArgs = new String[nCmd.size() + nArgs.size()];
        for (int i = 0; i < nCmd.size(); i++) {
            commandWithArgs[i] = nCmd.get(i);
        }

        for (int i = 0; i < nArgs.size(); i++) {
            commandWithArgs[i + nCmd.size()] = nArgs.get(i);
        }
        return commandWithArgs;
    }

    private static void processOutput(InputStream inputStream, Function<String, Void> function) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                function.apply(line);
            }
        }
    }

    private static List<Thread> startLoggingThreads(final Process process, final Logger log, final String commandDesc) {
        List<Thread> threads = new ArrayList<>();
        threads.add(startOutputLoggingThread(process, log, commandDesc));
        threads.add(startErrorLoggingThread(process, log, commandDesc));
        return threads;
    }

    private static Thread startErrorLoggingThread(final Process process, final Logger log, final String commandDesc) {
        Thread logThread = new Thread("[ERR] " + commandDesc) {
            @Override
            public void run() {
                try {
                    processOutput(process.getErrorStream(), createErrorHandler(log));
                } catch (IOException e) {
                    log.error("Failed to read error stream from [" + commandDesc + "] : [" + e.getMessage() + "]");
                }
            }
        };
        logThread.setDaemon(true);
        logThread.start();
        return logThread;
    }

    private static Thread startOutputLoggingThread(final Process process, final Logger log, final String commandDesc) {
        Thread logThread = new Thread("[OUT] " + commandDesc) {
            @Override
            public void run() {
                try {
                    processOutput(process.getInputStream(), createOutputHandler(log));
                } catch (IOException e) {
                    log.error("Failed to read output stream from [" + commandDesc + "] : [" + e.getMessage() + "]");
                }
            }
        };
        logThread.setDaemon(true);
        logThread.start();
        return logThread;
    }

    private static Function<String, Void> createOutputHandler(final Logger log) {
        return new Function<String, Void>() {
            @Override
            public Void apply(String outputLine) {
                log.info(outputLine);
                return null;
            }
        };
    }

    private static Function<String, Void> createErrorHandler(final Logger log) {
        return new Function<String, Void>() {
            @Override
            public Void apply(String outputLine) {
                log.error(outputLine);
                return null;
            }
        };
    }


    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    private static final String getCommand() {
        if (isWindows()) {
            return "cmd /c start";
        } else {
            return "/bin/sh -c";
        }
    }


    private static final String getSuffix() {
        if (isWindows()) {
            return ".bat";
        } else {
            return ".sh";
        }
    }
}
