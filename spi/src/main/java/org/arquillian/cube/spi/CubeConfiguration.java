package org.arquillian.cube.spi;

import java.util.Map;

public class CubeConfiguration {

    private static final String CONNECTION_MODE = "connectionMode";

    private ConnectionMode connectionMode = ConnectionMode.STARTANDSTOP;

    public ConnectionMode getConnectionMode() {
        return connectionMode;
    }

    public static CubeConfiguration fromMap(Map<String, String> map) {
        CubeConfiguration cubeConfiguration = new CubeConfiguration();

        if(map.containsKey(CONNECTION_MODE)) {
            cubeConfiguration.connectionMode = ConnectionMode.valueOf(ConnectionMode.class, map.get(CONNECTION_MODE));
        }
        return cubeConfiguration;
    }
}
