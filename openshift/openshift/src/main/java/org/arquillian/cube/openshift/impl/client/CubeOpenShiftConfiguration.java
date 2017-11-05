package org.arquillian.cube.openshift.impl.client;

import io.fabric8.kubernetes.clnt.v2_6.Config;
import io.fabric8.kubernetes.clnt.v2_6.ConfigBuilder;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import org.arquillian.cube.impl.util.Strings;
import org.arquillian.cube.kubernetes.impl.DefaultConfiguration;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.arquillian.cube.impl.util.ConfigUtil.asURL;
import static org.arquillian.cube.impl.util.ConfigUtil.getBooleanProperty;
import static org.arquillian.cube.impl.util.ConfigUtil.getIntProperty;
import static org.arquillian.cube.impl.util.ConfigUtil.getLongProperty;
import static org.arquillian.cube.impl.util.ConfigUtil.getStringProperty;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder.v2_6", generateBuilderPackage = false, editableEnabled = false, refs = {
    @BuildableReference(DefaultConfiguration.class)
})
public class CubeOpenShiftConfiguration extends DefaultConfiguration {

    private static final Config FALLBACK_CONFIG = new ConfigBuilder().build();

    //Deprecated Property Names: {
    private static final String ORIGIN_SERVER = "originServer";
    // }

    private static final String KEEP_ALIVE_GIT_SERVER = "keepAliveGitServer";
    private static final String DEFINITIONS_FILE = "definitionsFile";
    private static final String DEFINITIONS = "definitions";
    private static final String ENABLE_IMAGE_STREAM_DETECTION = "enableImageStreamDetection";
    private static final String AUTO_START_CONTAINERS = "autoStartContainers";
    private static final String PROXIED_CONTAINER_PORTS = "proxiedContainerPorts";
    private static final String PORT_FORWARDER_BIND_ADDRESS = "portForwardBindAddress";
    private static final String ROUTER_HOST = "routerHost";
    private static final String OPENSHIFT_ROUTER_HTTP_PORT = "openshiftRouterHttpPort";
    private static final String OPENSHIFT_ROUTER_HTTPS_PORT = "openshiftRouterHttpsPort";

    private final boolean keepAliveGitServer;
    private final String definitions;
    private final String definitionsFile;
    private final String[] autoStartContainers;
    private final Set<String> proxiedContainerPorts;
    private final String portForwardBindAddress;
    private final String routerHost;
    private final int openshiftRouterHttpPort;
    private final int openshiftRouterHttpsPort;
    private final boolean enableImageStreamDetection;

    public CubeOpenShiftConfiguration(String sessionId, URL masterUrl, String namespace, Map<String, String> scriptEnvironmentVariables, URL environmentSetupScriptUrl,
                                      URL environmentTeardownScriptUrl, URL environmentConfigUrl, List<URL> environmentDependencies,
                                      boolean namespaceLazyCreateEnabled, boolean namespaceCleanupEnabled, long namespaceCleanupTimeout,
                                      boolean namespaceCleanupConfirmationEnabled, boolean namespaceDestroyEnabled, long namespaceDestroyTimeout,
                                      boolean namespaceDestroyConfirmationEnabled, long waitTimeout, long waitPollInterval,
                                      List<String> waitForServiceList, boolean ansiLoggerEnabled, boolean environmentInitEnabled, boolean logCopyEnabled,
                                      String logPath, String kubernetesDomain, String dockerRegistry, boolean keepAliveGitServer, String definitions,
                                      String definitionsFile, String[] autoStartContainers, Set<String> proxiedContainerPorts,
                                      String portForwardBindAddress, String routerHost, int openshiftRouterHttpPort, int openshiftRouterHttpsPort, boolean enableImageStreamDetection) {
        super(sessionId, masterUrl, namespace, scriptEnvironmentVariables, environmentSetupScriptUrl, environmentTeardownScriptUrl,
            environmentConfigUrl, environmentDependencies, namespaceLazyCreateEnabled, namespaceCleanupEnabled,
            namespaceCleanupTimeout, namespaceCleanupConfirmationEnabled, namespaceDestroyEnabled,
            namespaceDestroyConfirmationEnabled, namespaceDestroyTimeout, waitTimeout, waitPollInterval,
            waitForServiceList, ansiLoggerEnabled, environmentInitEnabled, logCopyEnabled, logPath, kubernetesDomain, dockerRegistry);
        this.keepAliveGitServer = keepAliveGitServer;
        this.definitions = definitions;
        this.definitionsFile = definitionsFile;
        this.autoStartContainers = autoStartContainers;
        this.proxiedContainerPorts = proxiedContainerPorts;
        this.portForwardBindAddress = portForwardBindAddress;
        this.routerHost = routerHost;
        this.openshiftRouterHttpPort = openshiftRouterHttpPort;
        this.openshiftRouterHttpsPort = openshiftRouterHttpsPort;
        this.enableImageStreamDetection = enableImageStreamDetection;
    }

    private static String[] split(String str, String regex) {
        if (str == null || str.isEmpty()) {
            return new String[0];
        } else {
            return str.split(regex);
        }
    }

    public static CubeOpenShiftConfiguration fromMap(Map<String, String> map) {
        String sessionId = UUID.randomUUID().toString().split("-")[0];
        String namespace = getBooleanProperty(NAMESPACE_USE_CURRENT, map, false)
            ? new ConfigBuilder().build().getNamespace()
            : getStringProperty(NAMESPACE_TO_USE, map, null);

        //When a namespace is provided we want to cleanup our stuff...
        // ... without destroying pre-existing stuff.
        Boolean shouldDestroyNamespace = false;
        if (Strings.isNullOrEmpty(namespace)) {
            namespace = getStringProperty(NAMESPACE_PREFIX, map, "itest") + "-" + sessionId;
            shouldDestroyNamespace = true;
        }

        try {
            return new CubeOpenShiftConfigurationBuilder()
                .withSessionId(sessionId)
                .withNamespace(namespace)
                .withMasterUrl(
                    new URL(getStringProperty(MASTER_URL, KUBERNETES_MASTER, map, FALLBACK_CLIENT_CONFIG.getMasterUrl())))
                .withScriptEnvironmentVariables(parseMap(map.get(ENVIRONMENT_SCRIPT_ENV)))
                .withEnvironmentInitEnabled(getBooleanProperty(ENVIRONMENT_INIT_ENABLED, map, true))
                .withLogCopyEnabled(getBooleanProperty(LOGS_COPY, map, false))
                .withLogPath(getStringProperty(LOGS_PATH, map, null))
                .withEnvironmentSetupScriptUrl(
                    asUrlOrResource(getStringProperty(ENVIRONMENT_SETUP_SCRIPT_URL, map, null)))
                .withEnvironmentTeardownScriptUrl(
                    asUrlOrResource(getStringProperty(ENVIRONMENT_TEARDOWN_SCRIPT_URL, map, null)))
                .withEnvironmentConfigUrl(getKubernetesConfigurationUrl(map))
                .withEnvironmentDependencies(
                    asURL(Strings.splitAndTrimAsList(getStringProperty(ENVIRONMENT_DEPENDENCIES, map, ""), "\\s+")))
                .withNamespaceLazyCreateEnabled(
                    getBooleanProperty(NAMESPACE_LAZY_CREATE_ENABLED, map, DEFAULT_NAMESPACE_LAZY_CREATE_ENABLED))
                .withNamespaceCleanupEnabled(getBooleanProperty(NAMESPACE_CLEANUP_ENABLED, map, true))
                .withNamespaceCleanupConfirmationEnabled(
                    getBooleanProperty(NAMESPACE_CLEANUP_CONFIRM_ENABLED, map, false))
                .withNamespaceCleanupTimeout(
                    getLongProperty(NAMESPACE_CLEANUP_TIMEOUT, map, DEFAULT_NAMESPACE_CLEANUP_TIMEOUT))

                .withNamespaceDestroyEnabled(getBooleanProperty(NAMESPACE_DESTROY_ENABLED, map, shouldDestroyNamespace))
                .withNamespaceDestroyConfirmationEnabled(
                    getBooleanProperty(NAMESPACE_DESTROY_CONFIRM_ENABLED, map, false))
                .withNamespaceDestroyTimeout(
                    getLongProperty(NAMESPACE_DESTROY_TIMEOUT, map, DEFAULT_NAMESPACE_DESTROY_TIMEOUT))

                .withWaitTimeout(getLongProperty(WAIT_TIMEOUT, map, DEFAULT_WAIT_TIMEOUT))
                .withWaitPollInterval(getLongProperty(WAIT_POLL_INTERVAL, map, DEFAULT_WAIT_POLL_INTERVAL))
                .withWaitForServiceList(
                    Strings.splitAndTrimAsList(getStringProperty(WAIT_FOR_SERVICE_LIST, map, ""), "\\s+"))
                .withAnsiLoggerEnabled(getBooleanProperty(ANSI_LOGGER_ENABLED, map, true))
                .withKubernetesDomain(getStringProperty(DOMAIN, KUBERNETES_DOMAIN, map, null))
                .withDockerRegistry(getDockerRegistry(map))
                //Local properties
                .withKeepAliveGitServer(getBooleanProperty(KEEP_ALIVE_GIT_SERVER, map, false))
                .withDefinitions(getStringProperty(DEFINITIONS, map, null))
                .withDefinitionsFile(getStringProperty(DEFINITIONS_FILE, map, null))
                .withAutoStartContainers(split(getStringProperty(AUTO_START_CONTAINERS, map, ""), ","))
                .withProxiedContainerPorts(split(getStringProperty(PROXIED_CONTAINER_PORTS, map, ""), ","))
                .withPortForwardBindAddress(getStringProperty(PORT_FORWARDER_BIND_ADDRESS, map, "127.0.0.1"))
                .withRouterHost(getStringProperty(ROUTER_HOST, "openshift.router.host", map, null))
                .withOpenshiftRouterHttpPort(getIntProperty(OPENSHIFT_ROUTER_HTTP_PORT, Optional.of("openshift.router.httpPort"), map, 80))
                .withOpenshiftRouterHttpsPort(getIntProperty(OPENSHIFT_ROUTER_HTTPS_PORT, Optional.of("openshift.router.httpsPort"), map, 443))
                .withEnableImageStreamDetection(getBooleanProperty(ENABLE_IMAGE_STREAM_DETECTION, map, true))
                .build();
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                throw new RuntimeException(t);
            }
        }
    }

    public String getOriginServer() {
        return getMasterUrl().toString();
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
        if (autoStartContainers == null) {
            return new String[0];
        }
        return autoStartContainers;
    }

    public Set<String> getProxiedContainerPorts() {
        if (proxiedContainerPorts == null) {
            return Collections.emptySet();
        }
        return proxiedContainerPorts;
    }

    public String getPortForwardBindAddress() {
        return portForwardBindAddress;
    }

    public String getRouterHost() {
        return routerHost;
    }

    public int getOpenshiftRouterHttpPort() {
        return openshiftRouterHttpPort;
    }

    public int getOpenshiftRouterHttpsPort() {
        return openshiftRouterHttpsPort;
    }

    public boolean isEnableImageStreamDetection() {
        return enableImageStreamDetection;
    }
}
