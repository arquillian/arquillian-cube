package org.arquillian.cube.docker.impl.client.config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CubeContainer {

    private String containerName;
    private String workingDir;
    private Boolean disableNetwork;
    private String hostName;
    private Collection<String> portSpecs;
    private String user;
    private Boolean tty;
    private Boolean stdinOpen;
    private Boolean stdinOnce;
    private Long memoryLimit;
    private Long memorySwap;
    private Long shmSize;
    private Integer cpuShares;
    private Integer cpuQuota;
    private String cpuSet;
    private Boolean attachStdin;
    private Boolean attachSterr;
    private Collection<String> env;
    private Collection<String> cmd;
    private Collection<String> dns;
    private Collection<String> volumes;
    private Collection<String> volumesFrom;
    private Boolean removeVolumes = Boolean.TRUE;
    private Collection<String> binds;
    private Collection<Link> links;
    private Collection<String> dependsOn;
    private Collection<PortBinding> portBindings;
    private Collection<ExposedPort> exposedPorts;
    private Boolean privileged;
    private Boolean publishAllPorts;
    private String networkMode;
    private String ipv4Address;
    private String ipv6Address;
    private Collection<String> dnsSearch;
    private Collection<Device> devices;
    private RestartPolicy restartPolicy;
    private Collection<String> capAdd;
    private Collection<String> capDrop;
    private Collection<String> extraHosts;
    private Collection<String> entryPoint;
    private Collection<String> networks;
    private Collection<String> aliases;
    private String domainName;
    private Boolean alwaysPull = Boolean.FALSE;
    private boolean manual = false;
    private boolean killContainer = false;
    private Await await;

    private Image image;
    private String extendsImage;

    private boolean ReadonlyRootfs;
    private Map<String, String> labels;

    private BuildImage buildImage;

    private Collection<BeforeStop> beforeStop;

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public BuildImage getBuildImage() {
        return buildImage;
    }

    public void setBuildImage(BuildImage buildImage) {
        this.buildImage = buildImage;
    }

    public Collection<PortBinding> getPortBindings() {
        return portBindings;
    }

    public void setPortBindings(Collection<PortBinding> portBindings) {
        this.portBindings = portBindings;
    }

    public Collection<ExposedPort> getExposedPorts() {
        return exposedPorts;
    }

    public void setExposedPorts(Collection<ExposedPort> exposedPorts) {
        this.exposedPorts = exposedPorts;
    }

    public Boolean getReadonlyRootfs() {
        return ReadonlyRootfs;
    }

    public void setReadonlyRootfs(Boolean readonlyRootfs) {
        this.ReadonlyRootfs = readonlyRootfs;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }

    public Boolean getDisableNetwork() {
        return disableNetwork;
    }

    public void setDisableNetwork(Boolean disableNetwork) {
        this.disableNetwork = disableNetwork;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public Collection<String> getPortSpecs() {
        return portSpecs;
    }

    public void setPortSpecs(Collection<String> portSpecs) {
        this.portSpecs = portSpecs;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public Boolean getTty() {
        return tty;
    }

    public void setTty(Boolean tty) {
        this.tty = tty;
    }

    public Boolean getStdinOpen() {
        return stdinOpen;
    }

    public void setStdinOpen(Boolean stdinOpen) {
        this.stdinOpen = stdinOpen;
    }

    public Boolean getStdinOnce() {
        return stdinOnce;
    }

    public void setStdinOnce(Boolean stdinOnce) {
        this.stdinOnce = stdinOnce;
    }

    public Long getMemoryLimit() {
        return memoryLimit;
    }

    public void setMemoryLimit(Long memoryLimit) {
        this.memoryLimit = memoryLimit;
    }

    public Long getMemorySwap() {
        return memorySwap;
    }

    public void setMemorySwap(Long memorySwap) {
        this.memorySwap = memorySwap;
    }

    public Long getShmSize() {
        return shmSize;
    }

    public void setShmSize(Long shmSize) {
        this.shmSize = shmSize;
    }

    public Integer getCpuShares() {
        return cpuShares;
    }

    public void setCpuShares(Integer cpuShares) {
        this.cpuShares = cpuShares;
    }

    public String getCpuSet() {
        return cpuSet;
    }

    public void setCpuSet(String cpuSet) {
        this.cpuSet = cpuSet;
    }

    public Integer getCpuQuota() {
        return cpuQuota;
    }

    public void setCpuQuota(Integer cpuQuota) {
        this.cpuQuota = cpuQuota;
    }

    public Boolean getAttachStdin() {
        return attachStdin;
    }

    public void setAttachStdin(Boolean attachStdin) {
        this.attachStdin = attachStdin;
    }

    public Boolean getAttachSterr() {
        return attachSterr;
    }

    public void setAttachSterr(Boolean attachSterr) {
        this.attachSterr = attachSterr;
    }

    public Collection<String> getEnv() {
        return env;
    }

    public void setEnv(Collection<String> env) {
        this.env = env;
    }

    public Collection<String> getCmd() {
        return cmd;
    }

    public void setCmd(Collection<String> cmd) {
        this.cmd = cmd;
    }

    public Collection<String> getDns() {
        return dns;
    }

    public void setDns(Collection<String> dns) {
        this.dns = dns;
    }

    public Collection<String> getVolumes() {
        return volumes;
    }

    public void setVolumes(Collection<String> volumes) {
        this.volumes = volumes;
    }

    public Collection<String> getVolumesFrom() {
        return volumesFrom;
    }

    public void setVolumesFrom(Collection<String> volumesFrom) {
        this.volumesFrom = volumesFrom;
    }

    public Boolean getRemoveVolumes() {
        return removeVolumes;
    }

    public void setRemoveVolumes(Boolean deleteVolumes) {
        this.removeVolumes = deleteVolumes;
    }

    public Collection<String> getBinds() {
        return binds;
    }

    public void setBinds(Collection<String> binds) {
        this.binds = binds;
    }

    public Collection<Link> getLinks() {
        return links;
    }

    public void setLinks(Collection<Link> links) {
        this.links = links;
    }

    public Boolean getPrivileged() {
        return privileged;
    }

    public void setPrivileged(Boolean privileged) {
        this.privileged = privileged;
    }

    public Boolean getPublishAllPorts() {
        return publishAllPorts;
    }

    public void setPublishAllPorts(Boolean publishAllPorts) {
        this.publishAllPorts = publishAllPorts;
    }

    public String getNetworkMode() {
        return networkMode;
    }

    public void setNetworkMode(String networkMode) {
        this.networkMode = networkMode;
    }

    public Collection<String> getDnsSearch() {
        return dnsSearch;
    }

    public void setDnsSearch(Collection<String> dnsSearch) {
        this.dnsSearch = dnsSearch;
    }

    public Collection<Device> getDevices() {
        return devices;
    }

    public void setDevices(Collection<Device> devices) {
        this.devices = devices;
    }

    public Collection<String> getCapAdd() {
        return capAdd;
    }

    public void setCapAdd(Collection<String> capAdd) {
        this.capAdd = capAdd;
    }

    public Collection<String> getCapDrop() {
        return capDrop;
    }

    public void setCapDrop(Collection<String> capDrop) {
        this.capDrop = capDrop;
    }

    public Collection<String> getExtraHosts() {
        return extraHosts;
    }

    public void setExtraHosts(Collection<String> extraHosts) {
        this.extraHosts = extraHosts;
    }

    public Collection<String> getEntryPoint() {
        return entryPoint;
    }

    public void setEntryPoint(Collection<String> entryPoint) {
        this.entryPoint = entryPoint;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public Boolean getAlwaysPull() {
        return alwaysPull;
    }

    public void setAlwaysPull(Boolean alwaysPull) {
        this.alwaysPull = alwaysPull;
    }

    public RestartPolicy getRestartPolicy() {
        return restartPolicy;
    }

    public void setRestartPolicy(RestartPolicy restartPolicy) {
        this.restartPolicy = restartPolicy;
    }

    public Await getAwait() {
        return await;
    }

    public void setAwait(Await await) {
        this.await = await;
    }

    public boolean hasAwait() {
        return this.await != null;
    }

    public String getExtends() {
        return extendsImage;
    }

    public void setExtends(String extendsImage) {
        this.extendsImage = extendsImage;
    }

    public Collection<BeforeStop> getBeforeStop() {
        return beforeStop;
    }

    public void setBeforeStop(Collection<BeforeStop> beforeStop) {
        this.beforeStop = beforeStop;
    }

    public boolean hasBeforeStop() {
        return this.beforeStop != null && !this.beforeStop.isEmpty();
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public Collection<String> getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(Collection<String> dependsOn) {
        this.dependsOn = dependsOn;
    }

    public boolean isManual() {
        return manual;
    }

    public void setManual(boolean manual) {
        this.manual = manual;
    }

    public boolean isKillContainer() {
        return killContainer;
    }

    public void setKillContainer(boolean killContainer) {
        this.killContainer = killContainer;
    }

    public Collection<String> getNetworks() {
        return networks;
    }

    public void setNetworks(Collection<String> networks) {
        this.networks = networks;
    }

    public Collection<String> getAliases(){
        return aliases;
    }

    public void setAliases(Collection<String> aliases){
        this.aliases = aliases;
    }

    public Collection<String> getDependingContainers() {
        // Depends on has more priority than links
        if (dependsOn != null && dependsOn.size() > 0) {
            return Collections.unmodifiableCollection(dependsOn);
        } else {
            return getLinksName();
        }
    }

    private Collection<String> getLinksName() {
        if (links != null && links.size() > 0) {
            Set<String> dependencies = new HashSet<>();
            for (Link link : links) {
                dependencies.add(link.getName());
            }
            return dependencies;
        } else {
            return new ArrayList<>();
        }
    }

    public void merge(CubeContainer container) {
        try {
            Field[] fields = CubeContainer.class.getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }

                Object thisVal = field.get(this);
                if (thisVal == null) {
                    Object otherVal = field.get(container);
                    field.set(this, otherVal);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not merge objects", e);
        }
    }

    public String getIpv4Address() {
        return ipv4Address;
    }

    public void setIpv4Address(String ipv4Address) {
        this.ipv4Address = ipv4Address;
    }

    public String getIpv6Address() {
        return ipv6Address;
    }

    public void setIpv6Address(String ipv6Address) {
        this.ipv6Address = ipv6Address;
    }
}
