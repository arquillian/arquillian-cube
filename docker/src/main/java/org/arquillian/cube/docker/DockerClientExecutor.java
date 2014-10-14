package org.arquillian.cube.docker;

import java.util.List;
import java.util.Map;

import org.arquillian.cube.client.CubeConfiguration;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Device;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Link;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;
import com.github.dockerjava.api.model.RestartPolicy;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig.DockerClientConfigBuilder;

public class DockerClientExecutor {

    private static final String RESTART_POLICY = "restartPolicy";
    private static final String CAP_DROP = "capDrop";
    private static final String CAP_ADD = "capAdd";
    private static final String DEVICES = "devices";
    private static final String DNS_SEARCH = "dnsSearch";
    private static final String NETWORK_MODE = "networkMode";
    private static final String PUBLISH_ALL_PORTS = "publishAllPorts";
    private static final String PRIVILEGED = "privileged";
    private static final String PORT = "port";
    private static final String EXPOSED_PORT = "exposedPort";
    private static final String PORT_BINDINGS = "portBindings";
    private static final String LINKS = "links";
    private static final String BINDS = "binds";
    private static final String VOLUMES_FROM = "volumesFrom";
    private static final String VOLUMES = "volumes";
    private static final String DNS = "dns";
    private static final String CMD = "cmd";
    private static final String ENV = "env";
    private static final String ATTACH_STDERR = "attachStderr";
    private static final String ATTACH_STDIN = "attachStdin";
    private static final String CPU_SHARES = "cpuShares";
    private static final String MEMORY_SWAP = "memorySwap";
    private static final String MEMORY_LIMIT = "memoryLimit";
    private static final String STDIN_ONCE = "stdinOnce";
    private static final String STDIN_OPEN = "stdinOpen";
    private static final String TTY = "tty";
    private static final String USER = "user";
    private static final String PORT_SPECS = "portSpecs";
    private static final String HOST_NAME = "hostName";
    private static final String DISABLE_NETWORK = "disableNetwork";
    private static final String WORKING_DIR = "workingDir";
    private static final String EXPOSED_PORTS = "exposedPorts";
    private static final String IMAGE = "image";
    private DockerClient dockerClient;
    
    public DockerClientExecutor(CubeConfiguration cubeConfiguration) {
        DockerClientConfigBuilder configBuilder = DockerClientConfig
                .createDefaultConfigBuilder();
        configBuilder.withVersion(cubeConfiguration.getDockerServerVersion())
                       .withUri(cubeConfiguration.getDockerServerUri());
     
        this.dockerClient = DockerClientBuilder.getInstance(
                configBuilder.build()).build();
    }
    
    public CreateContainerResponse createContainer(String name, Map<String, Object> containerConfiguration) {
        String image = asString(containerConfiguration, IMAGE);
        
        CreateContainerCmd createContainerCmd = this.dockerClient.createContainerCmd(image);
        createContainerCmd.withName(name);
        
        if (containerConfiguration.containsKey(EXPOSED_PORTS)) {
            List<String> exposedPorts = asListOfString(
                    containerConfiguration, EXPOSED_PORTS);
            createContainerCmd.withExposedPorts(toExposedPorts(exposedPorts));
        }

        if (containerConfiguration.containsKey(WORKING_DIR)) {
            createContainerCmd.withWorkingDir(asString(containerConfiguration,
                    WORKING_DIR));
        }

        if (containerConfiguration.containsKey(DISABLE_NETWORK)) {
            createContainerCmd.withDisableNetwork(asBoolean(
                    containerConfiguration, DISABLE_NETWORK));
        }

        if (containerConfiguration.containsKey(HOST_NAME)) {
            createContainerCmd.withHostName(asString(containerConfiguration, HOST_NAME));
        }

        if (containerConfiguration.containsKey(PORT_SPECS)) {
            List<String> portSpecs = asListOfString(containerConfiguration,
                    PORT_SPECS);
            createContainerCmd.withPortSpecs(portSpecs.toArray(new String[portSpecs.size()]));
        }
        
        if(containerConfiguration.containsKey(USER)) {
            createContainerCmd.withUser(asString(containerConfiguration, USER));
        }
        
        if(containerConfiguration.containsKey(TTY)) {
            createContainerCmd.withTty(asBoolean(containerConfiguration, TTY));
        }
        
        if(containerConfiguration.containsKey(STDIN_OPEN)) {
           createContainerCmd.withStdinOpen(asBoolean(containerConfiguration, STDIN_OPEN));
        }
        
        if(containerConfiguration.containsKey(STDIN_ONCE)) {
            createContainerCmd.withStdInOnce(asBoolean(containerConfiguration, STDIN_ONCE));
        }
        
        if(containerConfiguration.containsKey(MEMORY_LIMIT)) {
            createContainerCmd.withMemoryLimit(asInt(containerConfiguration, MEMORY_LIMIT));
        }
        
        if(containerConfiguration.containsKey(MEMORY_SWAP)) {
            createContainerCmd.withMemorySwap(asInt(containerConfiguration, MEMORY_SWAP));
        }
        
        if(containerConfiguration.containsKey(CPU_SHARES)) {
            createContainerCmd.withCpuShares(asInt(containerConfiguration, CPU_SHARES));
        }
        
        if(containerConfiguration.containsKey(ATTACH_STDIN)) {
            createContainerCmd.withAttachStdin(asBoolean(containerConfiguration, ATTACH_STDIN));
        }
        
        if(containerConfiguration.containsKey(ATTACH_STDERR)) {
           createContainerCmd.withAttachStderr(asBoolean(containerConfiguration, ATTACH_STDERR));
        }
        
        if(containerConfiguration.containsKey(ENV)) {
            List<String> env = asListOfString(containerConfiguration, ENV);
            createContainerCmd.withEnv(env.toArray(new String[env.size()]));
        }
        
        if(containerConfiguration.containsKey(CMD)) {
            List<String> cmd = asListOfString(containerConfiguration, CMD);
            createContainerCmd.withCmd(cmd.toArray(new String[cmd.size()]));
        }
        
        if(containerConfiguration.containsKey(DNS)) {
            List<String> dns = asListOfString(containerConfiguration, DNS);
            createContainerCmd.withDns(dns.toArray(new String[dns.size()]));
        }
        
        if(containerConfiguration.containsKey(VOLUMES)) {
            List<String> volumes = asListOfString(containerConfiguration, VOLUMES);
            createContainerCmd.withVolumes(toVolumes(volumes));
        }
        
        if(containerConfiguration.containsKey(VOLUMES_FROM)) {
            List<String> volumesFrom = asListOfString(containerConfiguration, VOLUMES_FROM);
            createContainerCmd.withVolumesFrom(volumesFrom.toArray(new String[volumesFrom.size()]));
        }
        
        return createContainerCmd.exec();
    }
    
    public void startContainer(CreateContainerResponse createContainerResponse, Map<String, Object> containerConfiguration) {
        StartContainerCmd startContainerCmd = this.dockerClient.startContainerCmd(createContainerResponse.getId());
        
        if(containerConfiguration.containsKey(BINDS)) {
            List<String> binds = asListOfString(containerConfiguration, BINDS);
            startContainerCmd.withBinds(toBinds(binds));
        }
        
        if(containerConfiguration.containsKey(LINKS)) {
            startContainerCmd.withLinks(toLinks(asListOfString(containerConfiguration, LINKS)));
        }
        
        if(containerConfiguration.containsKey(PORT_BINDINGS)) {
            List<Map<String, Object>> portBindings = asListOfMap(containerConfiguration, PORT_BINDINGS);

            Ports ports = new Ports();
            for (Map<String, Object> map : portBindings) {
                if(map.containsKey(EXPOSED_PORT) && map.containsKey(PORT)) {
                    String exposedPort = asString(map, EXPOSED_PORT);
                    int port = asInt(map, PORT);
                    ports.bind(ExposedPort.parse(exposedPort), toBinding(Integer.toString(port)));
                }
            }
            startContainerCmd.withPortBindings(ports);
        }
        
        if(containerConfiguration.containsKey(PRIVILEGED)) {
            startContainerCmd.withPrivileged(asBoolean(containerConfiguration, PRIVILEGED));
        }
        
        if(containerConfiguration.containsKey(PUBLISH_ALL_PORTS)) {
            startContainerCmd.withPublishAllPorts(asBoolean(containerConfiguration, PUBLISH_ALL_PORTS));
        }
        
        if(containerConfiguration.containsKey(NETWORK_MODE)) {
            startContainerCmd.withNetworkMode(asString(containerConfiguration, NETWORK_MODE));
        }
        
        if(containerConfiguration.containsKey(DNS_SEARCH)) {
            List<String> dnsSearch = asListOfString(containerConfiguration, DNS_SEARCH);
            startContainerCmd.withDnsSearch(dnsSearch.toArray(new String[dnsSearch.size()]));
        }
        
        if(containerConfiguration.containsKey(DEVICES)) {
            
            List<Map<String, Object>> devices = asListOfMap(containerConfiguration, DEVICES);
            startContainerCmd.withDevices(toDevices(devices));
        }
        
        if(containerConfiguration.containsKey(RESTART_POLICY)) {
            Map<String, Object> restart = asMap(containerConfiguration, RESTART_POLICY);
            startContainerCmd.withRestartPolicy(toRestatPolicy(restart));
        }
        
        if(containerConfiguration.containsKey(CAP_ADD)) {
            List<String> capAdds = asListOfString(containerConfiguration, CAP_ADD);
            startContainerCmd.withCapAdd(capAdds.toArray(new String[capAdds.size()]));
        }

        if(containerConfiguration.containsKey(CAP_DROP)) {
            List<String> capDrop = asListOfString(containerConfiguration, CAP_DROP);
            startContainerCmd.withCapDrop(capDrop.toArray(new String[capDrop.size()]));
        }
        
        startContainerCmd.exec();
    }
    
    public void stopContainer(CreateContainerResponse createContainerResponse) {
        this.dockerClient.stopContainerCmd(createContainerResponse.getId()).exec();
    }
    
    public void removeContainer(CreateContainerResponse createContainerResponse) {
        this.dockerClient.removeContainerCmd(createContainerResponse.getId()).exec();
    }
    
    public InspectContainerResponse inspectContainer(CreateContainerResponse createContainerResponse) {
        return this.dockerClient.inspectContainerCmd(createContainerResponse.getId()).exec();
    }
    
    public int waitContainer(CreateContainerResponse createContainerResponse) {
        return this.dockerClient.waitContainerCmd(createContainerResponse.getId()).exec();
    }
    
    private static final Device[] toDevices(List<Map<String, Object>> devicesMap) {
        Device[] devices = new Device[devicesMap.size()];
        
        for (int i=0; i<devices.length; i++) {
            Map<String, Object> device = devicesMap.get(i);
            if(device.containsKey("cGroupPermissions") && device.containsKey("pathOnHost") && device.containsKey("pathInContainer")) {
                String cGroupPermissions = asString(device,"cGroupPermissions");
                String pathOnHost = asString(device, "pathOnHost");
                String pathInContainer = asString(device, "pathInContainer");
                
                devices[i] = new Device(cGroupPermissions, pathInContainer, pathOnHost);
                
            }
        }
        
        return devices;
    }
    
    private static final RestartPolicy toRestatPolicy(Map<String, Object> restart) {
        if(restart.containsKey("name")) {
            String name = asString(restart, "name");
            
            if("failure".equals(name)) {
                return RestartPolicy.onFailureRestart(asInt(restart, "maximumRetryCount"));
            } else {
                if("restart".equals(name)) {
                    return RestartPolicy.alwaysRestart();
                } else {
                    return RestartPolicy.noRestart();
                }
            }
            
        } else {
            return RestartPolicy.noRestart();
        }
    }
    
    private static final Binding toBinding(String port) {
        if(port.contains(":")) {
            String host = port.substring(0, port.lastIndexOf(":"));
            int extractedPort = Integer.parseInt(port.substring(port.lastIndexOf(":") + 1, port.length()));
            return Ports.Binding(host, extractedPort);
        } else {
            return Ports.Binding(Integer.parseInt(port));
        }
    }
    
    private static final boolean asBoolean(Map<String, Object> map, String property) {
        return (boolean)map.get(property);
    }
    
    private static final int asInt(Map<String, Object> map, String property) {
        return (int)map.get(property);
    }
    
    private static final Link[] toLinks(List<String> linkList) {
        Link[] links = new Link[linkList.size()];
        for (int i=0; i<links.length; i++) {
            links[i] = Link.parse(linkList.get(i));
        }
        
        return links;
    }
    
    private static final Bind[] toBinds(List<String> bindsList) {
        
        Bind[] binds = new Bind[bindsList.size()];
        for (int i=0; i<binds.length; i++) {
            binds[i] = Bind.parse(bindsList.get(i));
        }
        
        return binds;
    }
    
    private static final ExposedPort[] toExposedPorts(List<String> exposedPortsList) {
        ExposedPort[] exposedPorts = new ExposedPort[exposedPortsList.size()];
        
        for(int i=0;i<exposedPorts.length;i++) {
            exposedPorts[i] = ExposedPort.parse(exposedPortsList.get(i));
        }
        
        return exposedPorts;
    }
    
    private static final Volume[] toVolumes(List<String> volumesList) {
        Volume[] volumes = new Volume[volumesList.size()];
        
        for (int i=0; i<volumesList.size(); i++) {
            volumes[i] = new Volume(volumesList.get(i));
        }
        
        return volumes;
    }
    
    private static final List<Map<String, Object>> asListOfMap(Map<String, Object> map, String property) {
        return (List<Map<String, Object>>) map.get(property);
    }
    
    private static final List<String> asListOfString(Map<String, Object> map, String property) {
        return (List<String>) map.get(property);
    }
    
    private static final String asString(Map<String, Object> map, String property) {
        return (String) map.get(property);
    }
    
    private static final Map<String, Object> asMap(Map<String, Object> map, String property) {
        return (Map<String, Object>) map.get(property);
    }
    
}
