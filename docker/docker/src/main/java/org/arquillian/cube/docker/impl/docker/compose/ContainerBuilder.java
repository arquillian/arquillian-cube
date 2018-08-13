package org.arquillian.cube.docker.impl.docker.compose;

import org.arquillian.cube.docker.impl.client.config.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    private static final String MEM_SWAP_LIMIT = "memswap_limit";
    private static final String SHM_SIZE = "shm_size";
    private static final String PRIVILEGED = "privileged";
    private static final String RESTART = "restart";
    private static final String STDIN_OPEN = "stdin_open";
    private static final String TTY = "tty";
    private static final String CPU_SHARES = "cpu_shares";
    private static final String CPU_SET = "cpuset";
    private static final String CPU_QUOTA = "cpu_quota";
    private static final String EXTRA_HOSTS = "extra_hosts";
    private static final String DEVICES = "devices";
    private static final String CONTAINERNAME = "container_name";
    private static final String DEPENDS_ON = "depends_on";
    private static final String CONTEXT = "context";
    private static final String NETWORKS = "networks";

    private static List<String> AVAILABLE_COMMANDS = Arrays.asList(IMAGE, BUILD, COMMAND, LINKS, EXTERNAL_LINKS, DOCKERFILE,
            EXTENDS, PORTS, EXPOSE, VOLUMES, VOLUMES_FROM, ENVIRONMENT, ENV_FILE, NET, DNS, CAP_ADD, CAP_DROP,
            DNS_SEARCH, WORKING_DIR, ENTRYPOINT, USER, HOSTNAME, MEM_LIMIT, PRIVILEGED, RESTART, STDIN_OPEN, TTY,
            CPU_SET, CPU_SHARES, CPU_QUOTA, EXTRA_HOSTS, DEVICES, CONTAINERNAME, DEPENDS_ON, MEM_SWAP_LIMIT, SHM_SIZE, NETWORKS);

    private static final Logger log = Logger.getLogger(ContainerBuilder.class.getName());

    private Random random = new Random();

    private CubeContainer configuration;
    private Path dockerComposeRootLocation;
    GitOperations gitOperations;

    public ContainerBuilder(Path dockerComposeRootLocation) {
        this(dockerComposeRootLocation, new CubeContainer());
    }

    protected ContainerBuilder(Path dockerComposeRootLocation, CubeContainer configuration) {
        this.dockerComposeRootLocation = dockerComposeRootLocation;
        this.configuration = configuration;
    }

    public CubeContainer build(Map<String, Object> dockerComposeContainerDefinition) {
        return build(dockerComposeContainerDefinition, null);
    }

    @SuppressWarnings("unchecked")
    public CubeContainer build(Map<String, Object> dockerComposeContainerDefinition, String version) {
        if(dockerComposeContainerDefinition.containsKey(EXTENDS)) {
            Map<String, Object> extendsDefinition = asMap(dockerComposeContainerDefinition, EXTENDS);
            this.extend(Paths.get(asString(extendsDefinition, "file")), asString(extendsDefinition, "service"));
        }
        if (dockerComposeContainerDefinition.containsKey(IMAGE)) {
            this.addImage(asString(dockerComposeContainerDefinition, IMAGE));
        }
        if (dockerComposeContainerDefinition.containsKey(BUILD)) {
            if (DockerComposeConverter.DOCKER_COMPOSE_VERSION_2_VALUE.equals(version)) {
                Object o = dockerComposeContainerDefinition.get(BUILD);
                if (o instanceof String) {
                    String dockerfile = dockerComposeContainerDefinition.containsKey(DOCKERFILE) ? asString(dockerComposeContainerDefinition, DOCKERFILE) : null;
                    this.addBuild(asString(dockerComposeContainerDefinition, BUILD), dockerfile);
                } else if (o instanceof Map) {
                    Map<String, Object> buildDefinition = asMap(dockerComposeContainerDefinition, BUILD);

                    String context = buildDefinition.containsKey(CONTEXT) ? asString(buildDefinition, CONTEXT) : null;

                    final String dockerfile = buildDefinition.containsKey(DOCKERFILE) ? asString(buildDefinition, DOCKERFILE) : null;
                    if (context != null) {
                        File directory = new File(context);
                        if (directory.isDirectory()) {
                            this.addBuild(asString(buildDefinition, CONTEXT),
                                    dockerfile);
                        } else {
                            // Make GitOperations lazy because it will help for testing purposes and to avoid instantiate JGit classes when Git is not required-
                            // In this way, if you don't need Git, you don't need to add jgit dependency
                            if (gitOperations == null) {
                                log.log(Level.INFO, String.format("Starting cloning git repository %s defined in docker-compose", context));
                                GitOperations gitOperations = new GitOperations();
                                File clonedDirectory = gitOperations.cloneRepo(context);
                                log.log(Level.INFO, String.format("Finished cloning git repository %s defined in docker-compose", context));
                                this.addBuild(clonedDirectory.getParentFile().getAbsolutePath(), dockerfile);
                            }
                        }
                    } else {
                        log.log(Level.WARNING, "build configuration is provided as object but no context definition is found.");
                    }
                }
            } else {
                String dockerfile = dockerComposeContainerDefinition.containsKey(DOCKERFILE) ? asString(dockerComposeContainerDefinition, DOCKERFILE) : null;
                this.addBuild(asString(dockerComposeContainerDefinition, BUILD), dockerfile);
            }
        }
        if (dockerComposeContainerDefinition.containsKey(COMMAND)) {
            if (dockerComposeContainerDefinition.get(COMMAND) instanceof List) {
                this.addCommands(asListOfString(dockerComposeContainerDefinition, COMMAND));
            } else {
                this.addCommand(asString(dockerComposeContainerDefinition, COMMAND));
            }
        }
        if (dockerComposeContainerDefinition.containsKey(DEPENDS_ON)) {
            this.addDependsOn(asListOfString(dockerComposeContainerDefinition, DEPENDS_ON));
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
            this.addBinds(asListOfString(dockerComposeContainerDefinition, VOLUMES));
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
            this.addMemLimit(asLong(dockerComposeContainerDefinition, MEM_LIMIT));
        }
        if (dockerComposeContainerDefinition.containsKey(MEM_SWAP_LIMIT)) {
            this.addMemSwapLimit(asLong(dockerComposeContainerDefinition, MEM_SWAP_LIMIT));
        }
        if (dockerComposeContainerDefinition.containsKey(SHM_SIZE)) {
            this.addShmSize(asLong(dockerComposeContainerDefinition, SHM_SIZE));
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
        if (dockerComposeContainerDefinition.containsKey(CPU_QUOTA)) {
            this.addCpuQuota(asInt(dockerComposeContainerDefinition, CPU_QUOTA));
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

        if (dockerComposeContainerDefinition.containsKey(CONTAINERNAME)) {
            this.addContainerName(asString(dockerComposeContainerDefinition, CONTAINERNAME));
        }

        if (dockerComposeContainerDefinition.containsKey(NETWORKS)) {
            if (dockerComposeContainerDefinition.get(NETWORKS) instanceof ArrayList) {
                this.addNetworks(asListOfString(dockerComposeContainerDefinition, NETWORKS));
            } else {
                Map<String, Object> networks = asMap(dockerComposeContainerDefinition, NETWORKS);
                this.addNetworks(networks.keySet());
            }

        }

        this.logUnsupportedOperations(dockerComposeContainerDefinition.keySet());
        return this.build();
    }

    private ContainerBuilder addNetworks(Collection<String> networks) {
        if (configuration.getNetworks() != null) {
            configuration.getNetworks().addAll(networks);
        } else {
            configuration.setNetworks(new HashSet<>(networks));
        }

        return this;
    }

    private ContainerBuilder addShmSize(long shmSize) {
        configuration.setShmSize(shmSize);
        return this;
    }

    private ContainerBuilder addMemSwapLimit(long memSwapLimit) {
        configuration.setMemorySwap(memSwapLimit);
        return this;
    }


    private ContainerBuilder addDevices(Collection<String> devices) {
        Collection<Device> devicesDefinition = new HashSet<Device>();
        for (String device : devices) {
            String[] deviceSplitted = device.split(":");
            Device def = new Device();
            switch(deviceSplitted.length) {
                case 3: {
                    ///dev/ttyUSB0:/dev/ttyUSB0:rw
                    def.setcGroupPermissions(deviceSplitted[2]);
                }
                case 2: {
                    ///dev/ttyUSB0:/dev/ttyUSB0
                    def.setPathOnHost(deviceSplitted[0]);
                    def.setPathInContainer(deviceSplitted[1]);
                    break;
                }
                default: {
                    throw new IllegalArgumentException(String.format("Device definition %s is incorrect. It should follow the format <hostPath>:<containerPath>:(optional)<permissions>", device));
                }
            }
            devicesDefinition.add(def);
        }
        configuration.setDevices(devicesDefinition);
        return this;
    }

    public ContainerBuilder addExtraHosts(Collection<String> extraHosts) {
        if (configuration.getExtraHosts() != null) {
            Collection<String> oldExtraHosts = configuration.getExtraHosts();
            oldExtraHosts.addAll(extraHosts);
        } else {
            configuration.setExtraHosts(new HashSet<String>(extraHosts));
        }
        return this;
    }

    public ContainerBuilder addImage(String image) {
        configuration.setImage(Image.valueOf(image));
        return this;
    }

    public ContainerBuilder addContainerName(String name) {
        configuration.setContainerName(name);
        return this;
    }

    public ContainerBuilder addReadOnly(boolean b) {
        configuration.setReadonlyRootfs(b);
        return this;
    }

    public ContainerBuilder addBuild(String buildPath, String dockerfile) {
        //. directory is the root of the project, but docker-compose . means relative to docker-compose file, so we resolve the full path
        //and to no add full path we relativize to docker-compose

        Path buildPathPath = Paths.get(buildPath);

        Path calculatedPath;
        if (buildPathPath.isAbsolute()) {
            calculatedPath = buildPathPath;
        } else {
            Path fullDirectory = this.dockerComposeRootLocation.resolve(buildPath);
            calculatedPath = this.dockerComposeRootLocation.relativize(fullDirectory);
        }

        BuildImage buildImage = new BuildImage(calculatedPath.toString(), dockerfile, true, true);
        configuration.setBuildImage(buildImage);
        return this;
    }

    public ContainerBuilder addCommand(String command) {
        addCommands(Arrays.asList(command.split("\\ ")));
        return this;
    }

    public ContainerBuilder addCommands(Collection<String> commands) {
        configuration.setCmd(commands);
        return this;
    }

    public ContainerBuilder addDependsOn(Collection<String> dependsOn) {
        Collection<String> listOfDependsOn = new HashSet<>();
        for (String link : dependsOn) {
            listOfDependsOn.add(link);
        }

        if (configuration.getDependsOn() != null) {
            Collection<String> oldDependsOn = configuration.getDependsOn();
            oldDependsOn.addAll(listOfDependsOn);
        } else {
            configuration.setDependsOn(listOfDependsOn);
        }
        return this;
    }

    public ContainerBuilder addLinks(Collection<String> links) {
        Collection<Link> listOfLinks = new HashSet<Link>();
        for (String link : links) {
            listOfLinks.add(Link.valueOf(link));
        }

        if (configuration.getLinks() != null) {
            Collection<Link> oldLinks = configuration.getLinks();
            oldLinks.addAll(listOfLinks);
        } else {
            configuration.setLinks(listOfLinks);
        }
        return this;
    }

    public ContainerBuilder addPorts(Collection<String> ports) {
        Collection<PortBinding> listOfPorts = new HashSet<>();
        for (String port : ports) {
            String[] elements = port.split(":");
            switch (elements.length) {
                case 1: {
                    //random host port
                    if (port.contains("-")) {
                        getExpandedPorts(port).stream()
                                .forEach(expandedPort -> listOfPorts.add(PortBinding.valueOf(getRandomPort() + "->" + expandedPort)));
                    } else {
                        listOfPorts.add(PortBinding.valueOf(getRandomPort() + "->" + elements[0]));
                    }
                    break;
                }
                case 2: {
                    //hostport:containerport
                    if (port.contains("-")) {

                        listOfPorts.addAll(addPairPortRange(elements[0], elements[1], null));

                    } else {
                        listOfPorts.add(PortBinding.valueOf(port.replaceAll(":", "->")));
                    }
                    break;
                }
                case 3: {
                    //host:hostport:containerport
                    if (port.contains("-")) {
                        listOfPorts.addAll(addPairPortRange(elements[1], elements[2], elements[0]));
                    } else {
                        listOfPorts.add(PortBinding.valueOf(elements[0] + ":" + elements[1] + "->" + elements[2]));
                    }
                    break;
                }
            }
        }

        if (configuration.getPortBindings() != null) {
            Collection<PortBinding> oldPortBindings = configuration.getPortBindings();
            oldPortBindings.addAll(listOfPorts);
        } else {
            configuration.setPortBindings(listOfPorts);
        }
        return this;
    }

    private Collection<PortBinding> addPairPortRange(String hostRangePorts, String containerRangePorts, String host) {
        Collection<PortBinding> listOfPorts = new ArrayList<>();
        final List<String> expandedHostPorts = getExpandedPorts(hostRangePorts);
        final List<String> expandedContainerPorts = getExpandedPorts(containerRangePorts);

        if (expandedContainerPorts.size() != expandedHostPorts.size()) {
            throw new IllegalArgumentException("Port ranges from host and container side should contain same number of ports");
        }

        for (int i=0; i < expandedHostPorts.size(); i++) {
            if (host == null) {
                listOfPorts.add(PortBinding.valueOf(expandedHostPorts.get(i) + "->" + expandedContainerPorts.get(i)));
            } else {
                listOfPorts.add(PortBinding.valueOf(host + ":" + expandedHostPorts.get(i) + "->" + expandedContainerPorts.get(i)));
            }
        }

        return listOfPorts;
    }

    private List<String> getExpandedPorts(String expression) {
        String[] portRange = expression.split("-");

        if (portRange.length != 2) {
            throw new IllegalArgumentException("Expected Port Range expression but found " + expression);
        }

        int initialPort = Integer.parseInt(portRange[0].trim());
        int endPort = Integer.parseInt(portRange[1].trim()) + 1;

        return IntStream.range(initialPort, endPort).boxed()
                .map(String::valueOf)
                .collect(Collectors.toList());

    }

    public ContainerBuilder addExpose(Collection<String> exposes) {
        Collection<ExposedPort> ports = new HashSet<ExposedPort>();
        for(String exposed : exposes) {
            ports.add(ExposedPort.valueOf(exposed));
        }
        if (configuration.getExposedPorts() != null) {
            Collection<ExposedPort> oldExposedPorts = configuration.getExposedPorts();
            oldExposedPorts.addAll(ports);
        } else {
            configuration.setExposedPorts(ports);
        }
        return this;
    }

    public ContainerBuilder addVolumes(Collection<String> volumes) {
        if (configuration.getVolumes() != null) {
            Collection<String> oldVolumes = configuration.getVolumes();
            oldVolumes.addAll(volumes);
        } else {
            configuration.setVolumes(new HashSet<>(volumes));
        }
        return this;
    }

    public ContainerBuilder addBinds(Collection<String> volumes) {
        if (configuration.getBinds() != null) {
            Collection<String> oldBinds = configuration.getBinds();
            oldBinds.addAll(volumes);
        } else {
            configuration.setBinds(new HashSet<>(volumes));
        }

        return this;
    }

    public ContainerBuilder addVolumesFrom(Collection<String> volumesFrom) {
        if (configuration.getVolumesFrom() != null) {
            Collection<String> oldVolumes = configuration.getVolumesFrom();
            oldVolumes.addAll(volumesFrom);
        } else {
            configuration.setVolumesFrom(new HashSet<String>(volumesFrom));
        }
        return this;
    }

    public ContainerBuilder addLabels(Map<String, String> labels) {
        //TODO now only support for array approach and not dictionary
        if (configuration.getLabels() != null) {
            Map<String, String> oldLabels = configuration.getLabels();
            oldLabels.putAll(labels);
        } else {
            configuration.setLabels(labels);
        }
        return this;
    }

    public ContainerBuilder addEnvironment(Collection<String> environments) {
        //TODO now only support for array approach
        addEnvironment(getProperties(environments));
        return this;
    }

    private void addEnvironment(Properties properties) {
        if (configuration.getEnv() != null) {
            Properties oldProperties = getProperties(configuration.getEnv());
            oldProperties.putAll(properties);
            configuration.setEnv(toEnvironment(oldProperties));
        } else {
            configuration.setEnv(toEnvironment(properties));
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
        configuration.setNetworkMode(net);
        return this;
    }

    public ContainerBuilder addDns(String dns) {
        configuration.setDns(Arrays.asList(dns));
        return this;
    }

    public ContainerBuilder addDns(Collection<String> dns) {
        if (configuration.getDns() != null) {
            Collection<String> oldDns = configuration.getDns();
            oldDns.addAll(dns);
        } else {
            configuration.setDns(new HashSet<String>(dns));
        }
        return this;
    }

    public ContainerBuilder addCapAdd(Collection<String> capAdds) {
        if (configuration.getCapAdd() != null) {
            Collection<String> oldCapAdd = configuration.getCapAdd();
            oldCapAdd.addAll(capAdds);
        } else {
            configuration.setCapAdd(new HashSet<String>(capAdds));
        }
        return this;
    }

    public ContainerBuilder addCapDrop(Collection<String> capDrops) {
        if (configuration.getCapDrop() != null) {
            Collection<String> oldCapDrops = configuration.getCapDrop();
            oldCapDrops.addAll(capDrops);
        } else {
            configuration.setCapDrop(new HashSet<String>(capDrops));
        }
        return this;
    }

    public ContainerBuilder addDnsSearch(String dnsSearch) {
        configuration.setDnsSearch(Arrays.asList(dnsSearch));
        return this;
    }

    public ContainerBuilder addDnsSearch(Collection<String> dnsSearch) {
        if (configuration.getDnsSearch() != null) {
            Collection<String> oldDnsSearch = configuration.getDnsSearch();
            oldDnsSearch.addAll(dnsSearch);
        } else {
            configuration.setDnsSearch(new HashSet<String>(dnsSearch));
        }
        return this;
    }

    public ContainerBuilder addCpuShares(int cpuShares) {
        configuration.setCpuShares(cpuShares);
        return this;
    }

    private ContainerBuilder addCpuSet(String cpuSet) {
        configuration.setCpuSet(cpuSet);
        return this;
    }

    public ContainerBuilder addCpuQuota(int cpuQuota) {
        configuration.setCpuQuota(cpuQuota);
        return this;
    }

    public ContainerBuilder addTty(boolean tty) {
        configuration.setTty(tty);
        return this;
    }

    public ContainerBuilder addStdinOpen(boolean stdinOpen) {
        configuration.setStdinOpen(stdinOpen);
        return this;
    }

    public ContainerBuilder extend(Path location, String service) {
        Path extendLocation = this.dockerComposeRootLocation.resolve(location);
        DockerCompositions dockerCompositions = DockerComposeConverter.create(extendLocation).convert();
        CubeContainer cubeContainer = dockerCompositions.getContainers().get(service);

        if (cubeContainer == null) {
            throw new IllegalArgumentException(String.format("Service name %s is not present at %s", service, extendLocation.toAbsolutePath()));
        } else {
            ContainerBuilder containerBuilder = new ContainerBuilder(dockerComposeRootLocation, configuration);
            configuration.merge(cubeContainer);
        }
        return this;
    }

    public ContainerBuilder addRestart(String restart) {
        RestartPolicy restartPolicy = new RestartPolicy();
        if (restart.startsWith("on-failure")) {
            String[] element = restart.split(":");
            if (element.length == 1) {
                restartPolicy.setName(restart);
                restartPolicy.setMaximumRetryCount(0);
            } else {
                if (element.length == 2) {
                    restartPolicy.setName(element[0]);
                    restartPolicy.setMaximumRetryCount(Integer.parseInt(element[1]));
                } else {
                    throw new IllegalArgumentException("on-failure restart should be on-failure or with optional retries on-failure:retries");
                }
            }
        } else {
            restartPolicy.setName(restart);
        }
        configuration.setRestartPolicy(restartPolicy);
        return this;
    }

    public ContainerBuilder addPrivileged(boolean privileged) {
        configuration.setPrivileged(privileged);
        return this;
    }

    public ContainerBuilder addMemLimit(Long memLimit) {
        configuration.setMemoryLimit(memLimit);
        return this;
    }

    public ContainerBuilder addDomainName(String domainName) {
        configuration.setDomainName(domainName);
        return this;
    }

    public ContainerBuilder addHostname(String hostname) {
        configuration.setHostName(hostname);
        return this;
    }

    public ContainerBuilder addUser(String user) {
        configuration.setUser(user);
        return this;
    }

    public ContainerBuilder addEntrypoint(String entrypoint) {
        configuration.setEntryPoint(Arrays.asList(entrypoint));
        return this;
    }

    public ContainerBuilder addWorkingDir(String workingDir) {
        configuration.setWorkingDir(workingDir);
        return this;
    }

    public CubeContainer buildFromExtension() {
        return this.configuration;
    }

    public CubeContainer build() {
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

    private Collection<String> toEnvironment(Properties properties) {
        Collection<String> listOfEnvironment = new HashSet<String>();
        Set<Entry<Object, Object>> entrySet = properties.entrySet();
        for (Entry<Object, Object> entry : entrySet) {
            listOfEnvironment.add(entry.getKey() + "=" + entry.getValue());
        }
        return listOfEnvironment;
    }

    private void logUnsupportedOperations(Set<String> keys) {
        for (String key : keys) {
            if(!AVAILABLE_COMMANDS.contains(key)) {
                log.info(String.format("Key: %s is not implemented in Cube.", keys));
            }
        }
    }
}

