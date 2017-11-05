package org.arquillian.cube.docker.impl.client;

import java.util.List;
import org.arquillian.cube.docker.impl.util.AutoStartOrderUtil;

public class DefaultDockerAutoStartOrder implements DockerAutoStartOrder {
    @Override
    public List<String[]> getAutoStartOrder(CubeDockerConfiguration config) {
        return AutoStartOrderUtil.getAutoStartOrder(config);
    }

    @Override
    public List<String[]> getAutoStopOrder(CubeDockerConfiguration config) {
        return AutoStartOrderUtil.getAutoStopOrder(config);
    }
}
