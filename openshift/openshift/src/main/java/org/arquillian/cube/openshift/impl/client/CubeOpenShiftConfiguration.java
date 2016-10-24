package org.arquillian.cube.openshift.impl.client;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.sundr.builder.annotations.Buildable;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.arquillian.cube.impl.util.ConfigUtil.getBooleanProperty;
import static org.arquillian.cube.impl.util.ConfigUtil.getStringProperty;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder", generateBuilderPackage = false, editableEnabled = false)
public class CubeOpenShiftConfiguration {

    private static final Config FALLBACK_CONFIG = new ConfigBuilder().build();

    //Deprecated Property Names: {
        private static final String ORIGIN_SERVER = "originServer";
    // }

    private static final String MASTER_URL = "master.url";
    private static final String NAMESPACE = "namespace";
    private static final String KEEP_ALIVE_GIT_SERVER = "keepAliveGitServer";
    private static final String DEFINITIONS_FILE = "definitionsFile";
    private static final String DEFINITIONS = "definitions";
    private static final String AUTO_START_CONTAINERS = "autoStartContainers";

    private final String originServer;
    private final String namespace;
    private final boolean keepAliveGitServer;
    private final String definitions;
    private final String definitionsFile;
    private final String[] autoStartContainers;

    public CubeOpenShiftConfiguration(String originServer, String namespace, boolean keepAliveGitServer, String definitions, String definitionsFile, String[] autoStartContainers) {
        this.originServer = originServer;
        this.namespace = namespace;
        this.keepAliveGitServer = keepAliveGitServer;
        this.definitions = definitions;
        this.definitionsFile = definitionsFile;
        this.autoStartContainers = autoStartContainers;
    }

    public String getOriginServer() {
        return originServer;
    }

    public String getNamespace() {
        return namespace;
    }

    public boolean isKeepAliveGitServer() {
        return keepAliveGitServer;
    }

    public String getDefinitionsFile() {
        return definitionsFile;
    }

    public String getDefinitions() {
        return definitions;
    }

    public boolean shouldKeepAliveGitServer() {
        return keepAliveGitServer;
    }

    public String[] getAutoStartContainers() {
        if(autoStartContainers == null) {
            return new String[0];
        }
        return autoStartContainers;
    }

    private static String[] split(String str, String regex) {
        if (str == null || str.isEmpty()) {
            return new String[0];
        } else {
            return str.split(regex);
        }
    }

    public static CubeOpenShiftConfiguration fromMap(Map<String, String> map) {
        try {
            CubeOpenShiftConfiguration conf = new CubeOpenShiftConfigurationBuilder()
                    .withOriginServer(getStringProperty(MASTER_URL, ORIGIN_SERVER, map, FALLBACK_CONFIG.getMasterUrl()))
                    .withNamespace(getStringProperty(NAMESPACE, map, FALLBACK_CONFIG.getNamespace()))
                    .withKeepAliveGitServer(getBooleanProperty(KEEP_ALIVE_GIT_SERVER, map, false))
                    .withDefinitions(getStringProperty(DEFINITIONS, map, null))
                    .withDefinitionsFile(getStringProperty(DEFINITIONS_FILE, map, null))
                    .withAutoStartContainers(split(getStringProperty(AUTO_START_CONTAINERS, map, ""), ","))
                    .build();

            if (conf.getDefinitions() == null && conf.getDefinitionsFile() == null) {
                throw new IllegalArgumentException(
                        DEFINITIONS + " or " + DEFINITIONS_FILE + " configuration option is required");
            }
            if (conf.getDefinitionsFile() != null) {
                if (!new File(conf.definitionsFile).exists()) {
                    throw new IllegalArgumentException("No " + DEFINITIONS_FILE + " file found at " + conf.definitionsFile);
                }
            }
            return conf;
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                throw new RuntimeException(t);
            }
        }
    }
}
