package org.arquillian.cube.docker.impl.client.config;

public class RestartPolicy {

    private String name;
    private Integer maximumRetryCount;

    public RestartPolicy() { }

    public RestartPolicy(String name, Integer maximumRetryCount) {
        super();
        this.name = name;
        this.maximumRetryCount = maximumRetryCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMaximumRetryCount() {
        return maximumRetryCount;
    }

    public void setMaximumRetryCount(Integer maximumRetryCount) {
        this.maximumRetryCount = maximumRetryCount;
    }
}
