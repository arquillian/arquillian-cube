package org.arquillian.cube.docker.impl.client.config;

import java.util.List;
import java.util.Map;

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

    // custom
    private Object customProperties; // Anything provided by the user

    // log
    private String match;
    private boolean stdOut = true;
    private boolean stdErr;

    //http
    private Integer responseCode;
    private Map<String, Object> headers;
    private String url;

    //waitforit and log
    private Integer timeout = 15;

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

    public void setCustomProperties(Object customProperties) {
        this.customProperties = customProperties;
    }

    public Object getCustomProperties() {
        return customProperties;
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

    public String getMatch() {
        return match;
    }

    public void setMatch(String match) {
        this.match = match;
    }

    public boolean isStdOut() {
        return stdOut;
    }

    public void setStdOut(boolean stdOut) {
        this.stdOut = stdOut;
    }

    public boolean isStdErr() {
        return stdErr;
    }

    public void setStdErr(boolean stdErr) {
        this.stdErr = stdErr;
    }

    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public void setResponseCode(Integer responseCode) {
        this.responseCode = responseCode;
    }

    public Integer getResponseCode() {
        return responseCode;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }
}
