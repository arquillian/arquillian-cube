package org.arquillian.cube.docker.impl.docker.compose;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.yaml.snakeyaml.Yaml;

import static org.arquillian.cube.docker.impl.util.YamlUtil.*;


public class ContainerBuilder {

    private static final String IMAGE = "image";
    private static final String BUILD = "build";
    private static final String COMMAND = "command";
    private static final String LINKS = "links";
    private static final String EXTERNAL_LINKS = "external_links";
    private static final String LABELS = "labels";
    private static final String LOG_DRIVER = "log_driver";
    private static final String SECURITY_OPT = "security_opt";
    private static final String DOCKERFILE= "dockerfile";
    private static final String READ_ONLY = "read_only";
    private static final String EXTENDS = "extends";
    private static final String PORTS = "ports";
    private static final String EXPOSE = "expose";
    private static final String VOLUMES = "volumes";
    private static final String VOLUMES_FROM = "volumes_from";
    private static final String ENVIRONMENT = "environment";
    private static final String ENV_FILE = "env_file";
    private static final String NET = "net";
    private static final String DNS = "dns";
    private static final String CAP_ADD = "cap_add";
    private static final String CAP_DROP = "cap_drop";
    private static final String DNS_SEARCH = "dns_search";
    private static final String WORKING_DIR = "working_dir";
    private static final String ENTRYPOINT = "entrypoint";
    private static final String PID = "pid";
    private static final String USER = "user";
    private static final String HOSTNAME = "hostname";
    private static final String DOMAINNAME = "domainname";
    private static final String MEM_LIMIT = "mem_limit";
    private static final String PRIVILEGED = "privileged";
    private static final String RESTART = "restart";
    private static final String STDIN_OPEN = "stdin_open";
    private static final String TTY = "tty";
    private static final String CPU_SHARES = "cpu_shares";
    private static final String CPU_SET = "cpuset";
    private static final String EXTRA_HOSTS = "extra_hosts";
    private static final String DEVICES = "devices"; 

    private static List<String> AVAILABLE_COMMANDS = Arrays.asList(IMAGE, BUILD, COMMAND, LINKS, EXTERNAL_LINKS, DOCKERFILE,
            EXTENDS, PORTS, EXPOSE, VOLUMES, VOLUMES_FROM, ENVIRONMENT, ENV_FILE, NET, DNS, CAP_ADD, CAP_DROP,
            DNS_SEARCH, WORKING_DIR, ENTRYPOINT, USER, HOSTNAME, MEM_LIMIT, PRIVILEGED, RESTART, STDIN_OPEN, TTY,
            CPU_SET, CPU_SHARES, EXTRA_HOSTS, DEVICES);

    private static final Logger log = Logger.getLogger(ContainerBuilder.class.getName());

    private Random random = new Random();

    private Map<String, Object> configuration;
    private Path dockerComposeRootLocation;

    public ContainerBuilder(Path dockerComposeRootLocation) {
        this(dockerComposeRootLocation, new HashMap<String, Object>());
    }

    protected ContainerBuilder(Path dockerComposeRootLocation, Map<String, Object> configuration) {
        this.dockerComposeRootLocation = dockerComposeRootLocation;
        this.configuration = configuration;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> build(Map<String, Object> dockerComposeContainerDefinition) {
        if(dockerComposeContainerDefinition.containsKey(EXTENDS)) {
            Map<String, Object> extendsDefinition = asMap(dockerComposeContainerDefinition, EXTENDS);
            this.extend(Paths.get(asString(extendsDefinition, "file")), asString(extendsDefinition, "service"));
        }
        if (dockerComposeContainerDefinition.containsKey(IMAGE)) {
            this.addImage(asString(dockerComposeContainerDefinition, IMAGE));
        }
        if (dockerComposeContainerDefinition.containsKey(BUILD)) {
            String dockerfile = dockerComposeContainerDefinition.containsKey(DOCKERFILE) ? asString(dockerComposeContainerDefinition, DOCKERFILE) : null;
            this.addBuild(asString(dockerComposeContainerDefinition, BUILD), dockerfile);
        }
        if (dockerComposeContainerDefinition.containsKey(COMMAND)) {
            this.addCommand(asString(dockerComposeContainerDefinition, COMMAND));
        }
        if (dockerComposeContainerDefinition.containsKey(LINKS)) {
            this.addLinks(asListOfString(dockerComposeContainerDefinition, LINKS));
        }
        if (dockerComposeContainerDefinition.containsKey(EXTERNAL_LINKS)) {
            this.addLinks(asListOfString(dockerComposeContainerDefinition, EXTERNAL_LINKS));
        }
        if (dockerComposeContainerDefinition.containsKey(PORTS)) {
            this.addPorts(asListOfString(dockerComposeContainerDefinition, PORTS));
        }
        if (dockerComposeContainerDefinition.containsKey(EXPOSE)) {
            this.addExpose(asListOfString(dockerComposeContainerDefinition, EXPOSE));
        }
        if (dockerComposeContainerDefinition.containsKey(VOLUMES)) {
            this.addVolumes(asListOfString(dockerComposeContainerDefinition, VOLUMES));
        }
        if(dockerComposeContainerDefinition.containsKey(LABELS)) {
            this.addLabels(asMapOfStrings(dockerComposeContainerDefinition, LABELS));
        }
        if (dockerComposeContainerDefinition.containsKey(VOLUMES_FROM)) {
            this.addVolumesFrom(asListOfString(dockerComposeContainerDefinition, VOLUMES_FROM));
        }
        if (dockerComposeContainerDefinition.containsKey(ENVIRONMENT)) {
            this.addEnvironment(asListOfString(dockerComposeContainerDefinition, ENVIRONMENT));
        }
        if (dockerComposeContainerDefinition.containsKey(ENV_FILE)) {
            if(dockerComposeContainerDefinition.get(ENV_FILE) instanceof List) {
                this.addEnvFile(asListOfString(dockerComposeContainerDefinition, ENV_FILE));
            } else {
                this.addEnvFile(Arrays.asList(asString(dockerComposeContainerDefinition, ENV_FILE)));
            }
        }
        if (dockerComposeContainerDefinition.containsKey(NET)) {
            this.addNet(asString(dockerComposeContainerDefinition, NET));
        }
        if (dockerComposeContainerDefinition.containsKey(EXTRA_HOSTS)) {
            this.addExtraHosts(asListOfString(dockerComposeContainerDefinition, EXTRA_HOSTS));
        }
        if (dockerComposeContainerDefinition.containsKey(DNS)) {
            Object dns = dockerComposeContainerDefinition.get(DNS);
            if (dns instanceof List) {
                this.addDns((List<String>) dns);
            } else {
                this.addDns((String) dns);
            }
        }
        if (dockerComposeContainerDefinition.containsKey(CAP_ADD)) {
            this.addCapAdd(asListOfString(dockerComposeContainerDefinition, CAP_ADD));
        }
        if (dockerComposeContainerDefinition.containsKey(CAP_DROP)) {
            this.addCapDrop(asListOfString(dockerComposeContainerDefinition, CAP_DROP));
        }
        if (dockerComposeContainerDefinition.containsKey(DNS_SEARCH)) {
            Object dns = dockerComposeContainerDefinition.get(DNS_SEARCH);
            if (dns instanceof List) {
                this.addDnsSearch((List<String>) dns);
            } else {
                this.addDnsSearch((String) dns);
            }
        }
        if (dockerComposeContainerDefinition.containsKey(WORKING_DIR)) {
            this.addWorkingDir(asString(dockerComposeContainerDefinition, WORKING_DIR));
        }
        if (dockerComposeContainerDefinition.containsKey(ENTRYPOINT)) {
            this.addEntrypoint(asString(dockerComposeContainerDefinition, ENTRYPOINT));
        }
        if (dockerComposeContainerDefinition.containsKey(USER)) {
            this.addUser(asString(dockerComposeContainerDefinition, USER));
        }
        if (dockerComposeContainerDefinition.containsKey(HOSTNAME)) {
            this.addHostname(asString(dockerComposeContainerDefinition, HOSTNAME));
        }
        if (dockerComposeContainerDefinition.containsKey(MEM_LIMIT)) {
            this.addMemLimit(asInt(dockerComposeContainerDefinition, MEM_LIMIT));
        }
        if (dockerComposeContainerDefinition.containsKey(PRIVILEGED)) {
            this.addPrivileged(asBoolean(dockerComposeContainerDefinition, PRIVILEGED));
        }
        if (dockerComposeContainerDefinition.containsKey(RESTART)) {
            this.addRestart(asString(dockerComposeContainerDefinition, RESTART));
        }
        if (dockerComposeContainerDefinition.containsKey(STDIN_OPEN)) {
            this.addStdinOpen(asBoolean(dockerComposeContainerDefinition, STDIN_OPEN));
        }
        if (dockerComposeContainerDefinition.containsKey(TTY)) {
            this.addTty(asBoolean(dockerComposeContainerDefinition, TTY));
        }
        if (dockerComposeContainerDefinition.containsKey(CPU_SHARES)) {
            this.addCpuShares(asInt(dockerComposeContainerDefinition, CPU_SHARES));
        }
        if (dockerComposeContainerDefinition.containsKey(CPU_SET)) {
            this.addCpuSet(asString(dockerComposeContainerDefinition, CPU_SET));
        }
        if (dockerComposeContainerDefinition.containsKey(DEVICES)) {
            this.addDevices(asListOfString(dockerComposeContainerDefinition, DEVICES));
        }
        if (dockerComposeContainerDefinition.containsKey(DOMAINNAME)) {
           this.addDomainName(asString(dockerComposeContainerDefinition, DOMAINNAME));
        }
        if (dockerComposeContainerDefinition.containsKey(READ_ONLY)) {
            this.addReadOnly(asBoolean(dockerComposeContainerDefinition, READ_ONLY));
        }


        this.logUsupportedOperations(dockerComposeContainerDefinition.keySet());
        return this.build();
    }

    private ContainerBuilder addDevices(Collection<String> devices) {
        Set<Map<String, Object>> devicesDefinition = new HashSet<>();
        for (String device : devices) {
            String[] deviceSplitted = device.split(":");
            Map<String, Object> def = new HashMap<>();
            switch(deviceSplitted.length) {
                case 3: {
                    ///dev/ttyUSB0:/dev/ttyUSB0:rw
                    def.put(DockerClientExecutor.C_GROUP_PERMISSIONS, deviceSplitted[2]);
                }
                case 2: {
                    ///dev/ttyUSB0:/dev/ttyUSB0
                    def.put(DockerClientExecutor.PATH_ON_HOST, deviceSplitted[0]);
                    def.put(DockerClientExecutor.PATH_IN_CONTAINER, deviceSplitted[1]);
                    break;
                }
                default: {
                    throw new IllegalArgumentException(String.format("Device definition %s is incorrect. It should follow the format <hostPath>:<containerPath>:(optional)<permissions>", device));
                }
            }
            devicesDefinition.add(def);
        }
        configuration.put(DockerClientExecutor.DEVICES, devicesDefinition);
        return this;
    }

    public ContainerBuilder addExtraHosts(Collection<String> extraHosts) {
        if (configuration.containsKey(DockerClientExecutor.EXTRA_HOSTS)) {
            Set<String> oldExtraHosts = (Set<String>) configuration.get(DockerClientExecutor.EXTRA_HOSTS);
            oldExtraHosts.addAll(extraHosts);
        } else {
            configuration.put(DockerClientExecutor.EXPOSED_PORTS, new HashSet<>(extraHosts));
        }
        return this;
    }

    public ContainerBuilder addImage(String image) {
        configuration.put(DockerClientExecutor.IMAGE, image);
        return this;
    }

    public ContainerBuilder addReadOnly(boolean b) {
        configuration.put(DockerClientExecutor.READ_ONLY_ROOT_FS, b);
        return this;
    }

    public ContainerBuilder addBuild(String buildPath, String dockerfile) {
        Map<String, Object> buildImage = new HashMap<>();
        //. directory is the root of the project, but docker-compose . means relative to docker-compose file, so we resolve the full path
        //and to no add full path we relativize to docker-compose

        Path fullDirectory = this.dockerComposeRootLocation.resolve(buildPath);
        Path relativize = this.dockerComposeRootLocation.relativize(fullDirectory);
        buildImage.put(DockerClientExecutor.DOCKERFILE_LOCATION, relativize.toString());
        buildImage.put(DockerClientExecutor.NO_CACHE, true);
        buildImage.put(DockerClientExecutor.REMOVE, true);
        if(dockerfile != null) {
            buildImage.put(DockerClientExecutor.DOCKERFILE_NAME, dockerfile);
        }

        configuration.put(DockerClientExecutor.BUILD_IMAGE, buildImage);
        return this;
    }

    public ContainerBuilder addCommand(String command) {
        configuration.put(DockerClientExecutor.CMD, Arrays.asList(command));
        return this;
    }

    public ContainerBuilder addLinks(Collection<String> links) {
        List<String> listOfLinks = new ArrayList<String>();
        for (String link : links) {
            if (link.indexOf(':') == -1) {
                listOfLinks.add(link + ":" + link);
            } else {
                listOfLinks.add(link);
            }
        }

        if (configuration.containsKey(DockerClientExecutor.LINKS)) {
            Set<String> oldLinks = (Set) configuration.get(DockerClientExecutor.LINKS);
            oldLinks.addAll(listOfLinks);
        } else {
            configuration.put(DockerClientExecutor.LINKS, new HashSet<>(listOfLinks));
        }
        return this;
    }

    public ContainerBuilder addPorts(Collection<String> ports) {
        List<String> listOfPorts = new ArrayList<>();
        for (String port : ports) {
            String[] elements = port.split(":");
            switch (elements.length) {
                case 1: {
                    //random host port
                    listOfPorts.add(getRandomPort() + "->" + elements[0]);
                    break;
                }
                case 2: {
                    //hostport:containerport
                    listOfPorts.add(port.replaceAll(":", "->"));
                    break;
                }
                case 3: {
                    //host:hostport:containerport
                    listOfPorts.add(elements[0] + ":" + elements[1] + "->" + elements[2]);
                    break;
                }
            }
        }

        if (configuration.containsKey(DockerClientExecutor.PORT_BINDINGS)) {
            Set<String> oldPortBindings = (Set) configuration.get(DockerClientExecutor.PORT_BINDINGS);
            oldPortBindings.addAll(listOfPorts);
        } else {
            configuration.put(DockerClientExecutor.PORT_BINDINGS, new HashSet<>(listOfPorts));
        }
        return this;
    }

    public ContainerBuilder addExpose(Collection<String> exposes) {
        if (configuration.containsKey(DockerClientExecutor.EXPOSED_PORTS)) {
            Set<String> oldExposedPorts = (Set) configuration.get(DockerClientExecutor.EXPOSED_PORTS);
            oldExposedPorts.addAll(exposes);
        } else {
            configuration.put(DockerClientExecutor.EXPOSED_PORTS, new HashSet<>(exposes));
        }
        return this;
    }

    public ContainerBuilder addVolumes(Collection<String> volumes) {
        if (configuration.containsKey(DockerClientExecutor.VOLUMES)) {
            Set<String> oldVolumes = (Set) configuration.get(DockerClientExecutor.VOLUMES);
            oldVolumes.addAll(volumes);
        } else {
            configuration.put(DockerClientExecutor.VOLUMES, new HashSet<>(volumes));
        }
        return this;
    }

    public ContainerBuilder addVolumesFrom(Collection<String> volumesFrom) {
        if (configuration.containsKey(DockerClientExecutor.VOLUMES_FROM)) {
            Set<String> oldVolumes = (Set) configuration.get(DockerClientExecutor.VOLUMES_FROM);
            oldVolumes.addAll(volumesFrom);
        } else {
            configuration.put(DockerClientExecutor.VOLUMES_FROM, new HashSet<>(volumesFrom));
        }
        return this;
    }

    public ContainerBuilder addLabels(Map<String, String> labels) {
        //TODO now only support for array approach and not dictionary
        if (configuration.containsKey(DockerClientExecutor.LABELS)) {
            Map<String, String> oldLabels = (Map<String, String>) configuration.get(DockerClientExecutor.LABELS);
            oldLabels.putAll(labels);
        } else {
            configuration.put(DockerClientExecutor.LABELS, labels);
        }
        return this;
    }

    public ContainerBuilder addEnvironment(Collection<String> environments) {
        //TODO now only support for array approach
        addEnvironment(getProperties(environments));
        return this;
    }

    private void addEnvironment(Properties properties) {
        if (configuration.containsKey(DockerClientExecutor.ENV)) {
            Properties oldProperties = (Properties) configuration.get(DockerClientExecutor.ENV);
            oldProperties.putAll(properties);
        } else {
            configuration.put(DockerClientExecutor.ENV, properties);
        }
    }

    public ContainerBuilder addEnvFile(Collection<String> environmentPaths) {
        for (String environmentPath : environmentPaths) {
            try {
                Properties properties = new Properties();
                Path environmentLocation = Paths.get(environmentPath);
                File environmentFile = this.dockerComposeRootLocation.resolve(environmentLocation).toFile();
                FileInputStream inStream = new FileInputStream(environmentFile);
                properties.load(inStream);
                inStream.close();
                addEnvironment(properties);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return this;
    }

    public ContainerBuilder addNet(String net) {
        configuration.put(DockerClientExecutor.NETWORK_MODE, net);
        return this;
    }

    public ContainerBuilder addDns(String dns) {
        configuration.put(DockerClientExecutor.DNS, Arrays.asList(dns));
        return this;
    }

    public ContainerBuilder addDns(Collection<String> dns) {
        if (configuration.containsKey(DockerClientExecutor.DNS)) {
            Set<String> oldDns = (Set) configuration.get(DockerClientExecutor.DNS);
            oldDns.addAll(dns);
        } else {
            configuration.put(DockerClientExecutor.DNS, new HashSet<>(dns));
        }
        return this;
    }

    public ContainerBuilder addCapAdd(Collection<String> capAdds) {
        if (configuration.containsKey(DockerClientExecutor.CAP_ADD)) {
            Set<String> oldCapAdd = (Set) configuration.get(DockerClientExecutor.CAP_ADD);
            oldCapAdd.addAll(capAdds);
        } else {
            configuration.put(DockerClientExecutor.CAP_ADD, new HashSet<>(capAdds));
        }
        return this;
    }

    public ContainerBuilder addCapDrop(Collection<String> capDrops) {
        if (configuration.containsKey(DockerClientExecutor.CAP_DROP)) {
            Set<String> oldCapDrops = (Set) configuration.get(DockerClientExecutor.CAP_DROP);
            oldCapDrops.addAll(capDrops);
        } else {
            configuration.put(DockerClientExecutor.CAP_DROP, new HashSet<>(capDrops));
        }
        return this;
    }

    public ContainerBuilder addDnsSearch(String dnsSearch) {
        configuration.put(DockerClientExecutor.DNS_SEARCH, Arrays.asList(dnsSearch));
        return this;
    }

    public ContainerBuilder addDnsSearch(Collection<String> dnsSearch) {
        if (configuration.containsKey(DockerClientExecutor.DNS_SEARCH)) {
            Set<String> oldDnsSearch = (Set) configuration.get(DockerClientExecutor.DNS_SEARCH);
            oldDnsSearch.addAll(dnsSearch);
        } else {
            configuration.put(DockerClientExecutor.DNS_SEARCH, new HashSet<>(dnsSearch));
        }
        return this;
    }

    public ContainerBuilder addCpuShares(int cpuShares) {
        configuration.put(DockerClientExecutor.CPU_SHARES, cpuShares);
        return this;
    }

    private ContainerBuilder addCpuSet(String cpuSet) {
        configuration.put(DockerClientExecutor.CPU_SET, cpuSet);
        return this;
    }

    public ContainerBuilder addTty(boolean tty) {
        configuration.put(DockerClientExecutor.TTY, tty);
        return this;
    }

    public ContainerBuilder addStdinOpen(boolean stdinOpen) {
        configuration.put(DockerClientExecutor.STDIN_OPEN, stdinOpen);
        return this;
    }

    public ContainerBuilder extend(Path location, String service) {
        File extendLocation = this.dockerComposeRootLocation.resolve(location).toFile();
        try(FileInputStream inputStream = new FileInputStream(extendLocation)) {
            Map<String, Object> extendedDockerComposeFile = (Map<String, Object>) new Yaml().load(inputStream);
            Map<String, Object> serviceDockerComposeConfiguration = asMap(extendedDockerComposeFile, service);
            ContainerBuilder containerBuilder = new ContainerBuilder(dockerComposeRootLocation, configuration);
            configuration = containerBuilder.build(serviceDockerComposeConfiguration);

            if(serviceDockerComposeConfiguration == null) {
                throw new IllegalArgumentException(String.format("Service name %s is not present at %s", service, extendLocation.getAbsolutePath()));
            }

        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        return this;
    }

    public ContainerBuilder addRestart(String restart) {
        Map<String, Object> restartPolicy = new HashMap<>();
        if (restart.startsWith("on-failure")) {
            String[] element = restart.split(":");
            if (element.length == 1) {
                restartPolicy.put("name", restart);
                restartPolicy.put("maximumRetryCount", 0);
            } else {
                if (element.length == 2) {
                    restartPolicy.put("name", element[0]);
                    restartPolicy.put("maximumRetryCount", Integer.parseInt(element[1]));
                } else {
                    throw new IllegalArgumentException("on-failure restart should be on-failure or with optional retries on-failure:retries");
                }
            }
        } else {
            restartPolicy.put("name", restart);
        }
        configuration.put(DockerClientExecutor.RESTART_POLICY, restartPolicy);
        return this;
    }

    public ContainerBuilder addPrivileged(boolean privileged) {
        configuration.put(DockerClientExecutor.PRIVILEGED, privileged);
        return this;
    }

    public ContainerBuilder addMemLimit(int memLimit) {
        configuration.put(DockerClientExecutor.MEMORY_LIMIT, memLimit);
        return this;
    }

    public ContainerBuilder addDomainName(String domainName) {
        configuration.put(DockerClientExecutor.DOMAINNAME, domainName);
        return this;
    }

    public ContainerBuilder addHostname(String hostname) {
        configuration.put(DockerClientExecutor.HOST_NAME, hostname);
        return this;
    }

    public ContainerBuilder addUser(String user) {
        configuration.put(DockerClientExecutor.USER, user);
        return this;
    }

    public ContainerBuilder addEntrypoint(String entrypoint) {
        configuration.put(DockerClientExecutor.ENTRYPOINT, Arrays.asList(entrypoint));
        return this;
    }

    public ContainerBuilder addWorkingDir(String workingDir) {
        configuration.put(DockerClientExecutor.WORKING_DIR, workingDir);
        return this;
    }

    public Map<String, Object> buildFromExtension() {
        return this.configuration;
    }

    public Map<String, Object> build() {
        return configuration;
    }

    private String getRandomPort() {
        int shiftedRandomPort = random.nextInt(61000 - 32768);
        return Integer.toString(shiftedRandomPort + 32768);
    }

    private Properties getProperties(Collection<String> properties) {
        Properties allProperties = new Properties();
        for (String property : properties) {
            int keySeparator = property.indexOf("=");
            allProperties.put(property.substring(0, keySeparator), property.substring(keySeparator + 1, property.length()));
        }
        return allProperties;
    }

    private void logUsupportedOperations(Set<String> keys) {
        for (String key : keys) {
            if(!AVAILABLE_COMMANDS.contains(key)) {
                log.info(String.format("Key: %s is not implemented in Cube.", keys));
            }
        }
    }
}

