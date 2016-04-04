package org.arquillian.cube.docker.impl.client.config;

import java.util.List;

public class Await {

    private String strategy;

    // static
    private String ip;
    private List<Integer> ports;

    // polling
    private Object sleepPollingTime; // Integer or String expression
    private Integer iterations;
    private String type;

    // sleeping
    private Object sleepTime;

    public Await() {
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public List<Integer> getPorts() {
        return ports;
    }

    public void setPorts(List<Integer> ports) {
        this.ports = ports;
    }

    public Object getSleepPollingTime() {
        return sleepPollingTime;
    }

    public void setSleepPollingTime(Object sleepPollingTime) {
        this.sleepPollingTime = sleepPollingTime;
    }

    public Integer getIterations() {
        return iterations;
    }

    public void setIterations(Integer iterations) {
        this.iterations = iterations;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getSleepTime() {
        return sleepTime;
    }

    public void setSleepTime(Object sleepTime) {
        this.sleepTime = sleepTime;
    }
}
