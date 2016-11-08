package org.arquillian.cube.docker.impl.client.config;

import java.lang.reflect.Field;
import java.util.Map;

public class Network {

    private String driver;
    private IPAM ipam;
    private Map<String, String> options;

    public String getDriver() {
        return driver;
    }

    public IPAM getIpam() {
        return ipam;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public void setIpam(IPAM ipam) {
        this.ipam = ipam;
    }

    public void merge(Network container) {
        try {
            Field[] fields = Network.class.getDeclaredFields();
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

    public Map<String, String> getOptions() {
        return options;
    }

    public void setOptions(Map<String, String> options) {
        this.options = options;
    }
}
