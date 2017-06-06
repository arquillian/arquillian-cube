package org.arquillian.cube.openshift.impl.client;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.fabric8.kubernetes.clnt.v2_2.Config;
import io.fabric8.kubernetes.clnt.v2_2.ConfigBuilder;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

import org.arquillian.cube.impl.util.Strings;
import org.arquillian.cube.kubernetes.impl.DefaultConfiguration;

import static org.arquillian.cube.impl.util.ConfigUtil.asURL;
import static org.arquillian.cube.impl.util.ConfigUtil.getBooleanProperty;
import static org.arquillian.cube.impl.util.ConfigUtil.getLongProperty;
import static org.arquillian.cube.impl.util.ConfigUtil.getStringProperty;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder.v2_2", generateBuilderPackage = false, editableEnabled = false, refs = {
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
    private static final String AUTO_START_CONTAINERS = "autoStartContainers";
    private static final String PROXIED_CONTAINER_PORTS = "proxiedContainerPorts";
    private static final String PORT_FORWARDER_BIND_ADDRESS = "portForwardBindAddress";

    private static final String DEFAULT_OPENSHIFT_CONFIG_FILE_NAME = "openshift.json";

    private final boolean keepAliveGitServer;
    private final String definitions;
    private final String definitionsFile;
    private final String[] autoStartContainers;
    private final Set<String> proxiedContainerPorts;
    private final String portForwardBindAddress;

    public CubeOpenShiftConfiguration(String sessionId, URL masterUrl, String namespace, URL environmentSetupScriptUrl,
        URL environmentTeardownScriptUrl, URL environmentConfigUrl, List<URL> environmentConfigAdditionalUrls, List<URL> environmentDependencies,
        boolean namespaceLazyCreateEnabled, boolean namespaceCleanupEnabled, long namespaceCleanupTimeout,
        boolean namespaceCleanupConfirmationEnabled, boolean namespaceDestroyEnabled, long namespaceDestroyTimeout,
        boolean namespaceDestroyConfirmationEnabled, long waitTimeout, long waitPollInterval,
        List<String> waitForServiceList, boolean ansiLoggerEnabled, boolean environmentInitEnabled,
        String kubernetesDomain, String dockerRegistry, boolean keepAliveGitServer, String definitions,
        String definitionsFile, String[] autoStartContainers, Set<String> proxiedContainerPorts,
        String portForwardBindAddress) {
        super(sessionId, masterUrl, namespace, environmentSetupScriptUrl, environmentTeardownScriptUrl,
            environmentConfigUrl, environmentConfigAdditionalUrls, environmentDependencies, namespaceLazyCreateEnabled, namespaceCleanupEnabled,
            namespaceCleanupTimeout, namespaceCleanupConfirmationEnabled, namespaceDestroyEnabled,
            namespaceDestroyConfirmationEnabled, namespaceDestroyTimeout, waitTimeout, waitPollInterval,
            waitForServiceList, ansiLoggerEnabled, environmentInitEnabled, kubernetesDomain, dockerRegistry);
        this.keepAliveGitServer = keepAliveGitServer;
        this.definitions = definitions;
        this.definitionsFile = definitionsFile;
        this.autoStartContainers = autoStartContainers;
        this.proxiedContainerPorts = proxiedContainerPorts;
        this.portForwardBindAddress = portForwardBindAddress;
    }

    private static String[] split(String str, String regex) {
        if (str == null || str.isEmpty()) {
            return new String[0];
        } else {
            return str.split(regex);
        }
    }

    public static CubeOpenShiftConfiguration fromMap(Map<String, String> map) {
        String sessionId = UUID.randomUUID().toString();
        String namespace = getBooleanProperty(NAMESPACE_USE_CURRENT, map, false)
            ? new ConfigBuilder().build().getNamespace()
            : getStringProperty(NAMESPACE_TO_USE, map, null);

        //When a namespace is provided we want to cleanup our stuff...
        // ... without destroying pre-existing stuff.
        Boolean shouldCleanupNamespace = true;
        Boolean shouldDestroyNamespace = false;
        if (Strings.isNullOrEmpty(namespace)) {
            //When we generate a namespace ourselves we to completely destroy it, so cleaning makes no sense.
            namespace = getStringProperty(NAMESPACE_PREFIX, map, "itest") + "-" + sessionId;
            shouldDestroyNamespace = true;
            shouldCleanupNamespace = false;
        }

        // Lets also try to load the image stream for the project.
        List<URL> additionalUrls = new LinkedList<>();
        File targetDir = new File(System.getProperty("basedir", ".") + "/target");
         if (targetDir.exists() && targetDir.isDirectory()) {
            File[]files = targetDir.listFiles();
             if (files != null) {
                 for (File file : files) {
                     if (file.getName().endsWith("-is.yml")) {
                         try {
                             additionalUrls.add(file.toURI().toURL());
                         } catch (MalformedURLException e) {
                             // ignore
                         }
                    }
                }
            }
        }


        try {
            return new CubeOpenShiftConfigurationBuilder()
                .withSessionId(sessionId)
                .withNamespace(namespace)
                .withMasterUrl(
                    new URL(getStringProperty(MASTER_URL, KUBERNETES_MASTER, map, FALLBACK_CLIENT_CONFIG.getMasterUrl())))
                .withEnvironmentInitEnabled(getBooleanProperty(ENVIRONMENT_INIT_ENABLED, map, true))
                .withEnvironmentSetupScriptUrl(
                    asUrlOrResource(getStringProperty(ENVIRONMENT_SETUP_SCRIPT_URL, map, null)))
                .withEnvironmentTeardownScriptUrl(
                    asUrlOrResource(getStringProperty(ENVIRONMENT_TEARDOWN_SCRIPT_URL, map, null)))
                .withEnvironmentConfigUrl(getKubernetesConfigurationUrl(map, DEFAULT_OPENSHIFT_CONFIG_FILE_NAME))
                .withEnvironmentConfigAdditionalUrls(additionalUrls)
                .withEnvironmentDependencies(
                    asURL(Strings.splitAndTrimAsList(getStringProperty(ENVIRONMENT_DEPENDENCIES, map, ""), "\\s+")))
                .withNamespaceLazyCreateEnabled(
                    getBooleanProperty(NAMESPACE_LAZY_CREATE_ENABLED, map, DEFAULT_NAMESPACE_LAZY_CREATE_ENABLED))
                .withNamespaceCleanupEnabled(getBooleanProperty(NAMESPACE_CLEANUP_ENABLED, map, shouldCleanupNamespace))
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
}
