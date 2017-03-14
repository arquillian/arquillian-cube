package org.arquillian.cube.docker.impl.client.config;

import org.arquillian.cube.spi.metadata.CubeMetadata;
import org.arquillian.cube.spi.metadata.NetworkMetadata;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class Network {

    private Map<Class<? extends NetworkMetadata>, Object> metadata = new HashMap<>();

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

    public <X extends NetworkMetadata> boolean hasMetadata(Class<X> type) {
        return metadata.containsKey(type);
    }

    public <X extends NetworkMetadata> void addMetadata(Class<X> type, X impl) {
        metadata.put(type, impl);
    }

    public <X extends NetworkMetadata> X getMetadata(Class<X> type) {
        return (X) metadata.get(type);
    }
}
