package org.arquillian.cube.openshift.impl.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import io.fabric8.kubernetes.api.KubernetesHelper;

public class CubeOpenShiftConfiguration {

    private static final String ORIGIN_SERVER = "originServer";
    private static final String NAMESPACE = "namespace";
    private static final String KEEP_ALIVE_GIT_SERVER = "keepAliveGitServer";
    private static final String DEFINITIONS_FILE = "definitionsFile";
    private static final String DEFINITIONS = "definitions";
    private static final String AUTO_START_CONTAINERS = "autoStartContainers";

    private String originServer;
    private String namespace;
    private boolean keepAliveGitServer = true;
    private String definitions;
    private String definitionsFile;
    private String[] autoStartContainers;

    public String getOriginServer() {
        return originServer;
    }

    public String getNamespace() {
        return namespace;
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

    public static CubeOpenShiftConfiguration fromMap(Map<String, String> config) {

        CubeOpenShiftConfiguration conf = new CubeOpenShiftConfiguration();
        conf.originServer = getRequired(config, ORIGIN_SERVER);
        conf.namespace = getRequired(config, NAMESPACE);
        conf.definitions = config.get(DEFINITIONS);
        conf.definitionsFile = config.get(DEFINITIONS_FILE);
        if (config.containsKey(KEEP_ALIVE_GIT_SERVER)) {
            conf.keepAliveGitServer = Boolean.parseBoolean(config.get(KEEP_ALIVE_GIT_SERVER));
        }
        if (config.containsKey(AUTO_START_CONTAINERS)) {
            conf.autoStartContainers = config.get(AUTO_START_CONTAINERS).split(",");
        }
        if (conf.definitions == null && conf.definitionsFile == null) {
            throw new IllegalArgumentException(
                    DEFINITIONS + " or " + DEFINITIONS_FILE + " configuration option is required");
        }
        if (conf.definitionsFile != null) {
            if (!new File(conf.definitionsFile).exists()) {
                throw new IllegalArgumentException("No " + DEFINITIONS_FILE + " file found at " + conf.definitionsFile);
            }
        }
        return conf;
    }

    private static String getRequired(Map<String, String> config, String key) {
        if (!config.containsKey(key)) {
            throwOptionRequired(key);
        }
        return config.get(key);
    }

    private static void throwOptionRequired(String key) {
        throw new IllegalArgumentException(key + " configuration option is required");
    }

    public Object getDefinitions() {
        if (definitions != null) {
            try {
                return KubernetesHelper.loadJson(definitions);
            } catch (IOException e) {
                throw new RuntimeException("Could not read " + DEFINITIONS, e);
            }
        } else if (definitionsFile != null) {
            try {
                return KubernetesHelper.loadJson(new FileInputStream(definitionsFile));
            } catch (IOException e) {
                throw new RuntimeException("Could not read " + DEFINITIONS_FILE + " at " + definitionsFile, e);
            }
        }
        // Technically should not happen, validated during creation.
        throw new IllegalStateException(DEFINITIONS + " or " + DEFINITIONS_FILE + " not provided.");
    }
}
