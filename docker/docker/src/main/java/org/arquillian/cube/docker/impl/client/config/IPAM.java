package org.arquillian.cube.docker.impl.client.config;

import java.util.List;
import java.util.Map;

public class IPAM {

    private List<IPAMConfig> ipamConfigs;
    private Map<String, String> options;
    private String driver;

    public List<IPAMConfig> getIpamConfigs() {
        return ipamConfigs;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public void setIpamConfigs(List<IPAMConfig> ipamConfigs) {
        this.ipamConfigs = ipamConfigs;
    }

    public void setOptions(Map<String, String> options) {
        this.options = options;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }
}
