package org.arquillian.cube.docker.impl.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class DockerMachine extends AbstractCliInternetAddressResolver {

    public static final String DOCKER_MACHINE_EXEC = "docker-machine";
    private static final Pattern IP_PATTERN = Pattern.compile("(?:\\d{1,3}\\.){3}\\d{1,3}");
    private static Logger log = Logger.getLogger(DockerMachine.class.getName());
    private String machineName;
    private boolean manuallyStarted = false;

    public DockerMachine(CommandLineExecutor commandLineExecutor) {
        super(commandLineExecutor);
    }

    @Override
    protected String[] getCommandArguments(String cliPathExec) {
        if (machineName == null) {
            throw new IllegalArgumentException("Machine Name cannot be null and couldn't be autoresolved.");
        }

        return new String[] {createDockerMachineCommand(cliPathExec), "ip", machineName};
    }

    @Override
    protected Pattern getIpPattern() {
        return IP_PATTERN;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    public boolean isManuallyStarted() {
        return manuallyStarted;
    }

    /**
     * Starts given docker machine.
     *
     * @param cliPathExec
     *     location of docker-machine or null if it is on PATH.
     * @param machineName
     *     to be started.
     */
    public void startDockerMachine(String cliPathExec, String machineName) {
        commandLineExecutor.execCommand(createDockerMachineCommand(cliPathExec), "start", machineName);
        this.manuallyStarted = true;
    }

    /**
     * Starts given docker machine.
     *
     * @param machineName
     *     to be started.
     */
    public void startDockerMachine(String machineName) {
        startDockerMachine(null, machineName);
    }

    public void stopDockerMachine(String cliPathExec, String machineName) {
        commandLineExecutor.execCommand(createDockerMachineCommand(cliPathExec), "stop", machineName);
        this.manuallyStarted = false;
    }

    public void stopDockerMachine(String machineName) {
        stopDockerMachine(null, machineName);
    }

    /**
     * Checks if Docker Machine is installed by running docker-machine and inspect the result.
     *
     * @param cliPathExec
     *     location of docker-machine or null if it is on PATH.
     *
     * @return true if it is installed, false otherwise.
     */
    public boolean isDockerMachineInstalled(String cliPathExec) {
        try {
            commandLineExecutor.execCommand(createDockerMachineCommand(cliPathExec));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if Docker Machine is installed by running docker-machine and inspect the result.
     *
     * @return true if it is installed, false otherwise.
     */
    public boolean isDockerMachineInstalled() {
        return isDockerMachineInstalled(null);
    }

    /**
     * Executes docker-machine ls command
     *
     * @param cliPathExec
     *     location of docker-machine or null if it is on PATH.
     *
     * @return set of machines
     */
    public Set<Machine> list(String cliPathExec) {

        Set<Machine> machines = new HashSet<>();
        List<String> output = commandLineExecutor.execCommandAsArray(createDockerMachineCommand(cliPathExec), "ls");

        Map<String, Index> headerIndex = calculateStartingFieldsIndex(output.get(0));
        for (String fields : output.subList(1, output.size())) {
            machines.add(parse(headerIndex, fields));
        }

        return machines;
    }

    /**
     * Executes docker-machine ls --filter field=value command
     *
     * @param cliPathExec
     *     location of docker-machine or null if it is on PATH.
     * @param field
     *     to use in condition
     * @param value
     *     value that the field shoudl have
     *
     * @return set of machines
     */
    public Set<Machine> list(String cliPathExec, String field, String value) {
        final Set<Machine> machines = new HashSet<>();
        List<String> output =
            commandLineExecutor.execCommandAsArray(createDockerMachineCommand(cliPathExec), "ls", "--filter",
                field + "=" + value);
        output = findHeader(output);

        if (!output.isEmpty()) {
            final Map<String, Index> headerIndex = calculateStartingFieldsIndex(output.get(0));
            for (String fields : output.subList(1, output.size())) {
                machines.add(parse(headerIndex, fields));
            }
        }

        return machines;
    }

    private List<String> findHeader(List<String> output) {
        for (int i = 0; i < output.size(); i++) {
            if (output.get(i).startsWith("NAME")) {
                return output.subList(i, output.size());
            }
        }

        return output;
    }

    private Map<String, Index> calculateStartingFieldsIndex(String header) {
        Map<String, Index> headersIndex = new HashMap<>();
        String[] headers = header.split("\\s+");
        for (int i = 0; i < headers.length; i++) {
            String currentHeader = headers[i];
            int firstIndex = header.indexOf(currentHeader);
            int lastIndex = (i + 1 < headers.length) ? header.indexOf(headers[i + 1]) - 1 : -1;

            headersIndex.put(currentHeader, new Index(firstIndex, lastIndex));
        }
        return headersIndex;
    }

    /**
     * Executes docker-machine ls command
     *
     * @return set of machines
     */
    public Set<Machine> list() {
        return this.list(null);
    }

    /**
     * Executes docker-machine ls --filter field=value command
     *
     * @param field
     *     to use in condition
     * @param value
     *     value that the field shoudl have
     *
     * @return set of machines
     */
    public Set<Machine> list(String field, String value) {
        return this.list(null, field, value);
    }

    public void grantPermissionToDockerMachine(String machinePath) {
        List<String> chmod = commandLineExecutor.execCommandAsArray("chmod", "+x", machinePath);
        printOutput(chmod);
    }

    public void createMachine(String machinePath, String machineDriver, String machineName) {
        List<String> create =
            commandLineExecutor.execCommandAsArray(machinePath, "create", "--driver", machineDriver, machineName);
        printOutput(create);
    }

    private void printOutput(List<String> lines) {
        StringBuilder output = new StringBuilder();
        for (String line : lines) {
            output.append(line);
            output.append(System.lineSeparator());
        }
        log.info(output.toString());
    }

    private Machine parse(Map<String, Index> headersIndex, String output) {
        String name = resolveField(headersIndex.get("NAME"), output);
        String active = resolveField(headersIndex.get("ACTIVE"), output);
        String driver = resolveField(headersIndex.get("DRIVER"), output);
        String state = resolveField(headersIndex.get("STATE"), output);
        String url = resolveField(headersIndex.get("URL"), output);
        String swarm = resolveField(headersIndex.get("SWARM"), output);

        return new Machine(name, active, driver, state, url, swarm);
    }

    private String resolveField(Index index, String output) {
        if (index.getEndIndex() < 0) {
            if (index.getStartIndex() < 0) {
                return "";
            }
            return output.substring(index.getStartIndex(), output.length()).trim();
        } else {
            return output.substring(index.getStartIndex(), index.getEndIndex() + 1).trim();
        }
    }

    private String createDockerMachineCommand(String dockerMachinePath) {
        return dockerMachinePath == null ? DOCKER_MACHINE_EXEC : dockerMachinePath;
    }

    private class Index {
        private int startIndex = -1;
        private int endIndex = -1;

        public Index(int startIndex, int endIndex) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        public int getStartIndex() {
            return startIndex;
        }

        public int getEndIndex() {
            return endIndex;
        }
    }
}
