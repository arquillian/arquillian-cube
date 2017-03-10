package org.arquillian.cube.openshift.impl.client;

import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.impl.DefaultConfiguration;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.arquillian.cube.impl.util.ConfigUtil.getBooleanProperty;
import static org.arquillian.cube.impl.util.ConfigUtil.getStringProperty;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder", generateBuilderPackage = false, editableEnabled = false, refs = {
        @BuildableReference(DefaultConfiguration.class)
})
public class CubeOpenShiftConfiguration extends DefaultConfiguration {

    private static final Config FALLBACK_CONFIG = new ConfigBuilder().build();

    //Deprecated Property Names: {
        private static final String ORIGIN_SERVER = "originServer";
    // }

    private static final String KEEP_ALIVE_GIT_SERVER = "keepAliveGitServer";
    private static final String ENV_CONFIG_URL = "env.config.url";
    private static final String DEFINITIONS = "definitions";
    private static final String AUTO_START_CONTAINERS = "autoStartContainers";
    private static final String PROXIED_CONTAINER_PORTS = "proxiedContainerPorts";
    private static final String PORT_FORWARDER_BIND_ADDRESS = "portForwardBindAddress";


    private final boolean keepAliveGitServer;
    private final String definitions;
    private final String definitionsFile;
    private final String[] autoStartContainers;
    private final Set<String> proxiedContainerPorts;
    private final String portForwardBindAddress;

    public CubeOpenShiftConfiguration(String sessionId, URL masterUrl, String namespace, URL environmentConfigUrl, List<URL> environmentDependencies, boolean namespaceLazyCreateEnabled, boolean namespaceCleanupEnabled, long namespaceCleanupTimeout, boolean namespaceCleanupConfirmationEnabled,boolean namespaceDestroyEnabled, long namespaceDestroyTimeout, boolean namespaceDestroyConfirmationEnabled, long waitTimeout, long waitPollInterval, boolean waitForServiceConnectionEnabled, List<String> waitForServiceList, long waitForServiceConnectionTimeout, boolean ansiLoggerEnabled, boolean environmentInitEnabled, String kubernetesDomain, String dockerRegistry, boolean keepAliveGitServer, String definitions, String definitionsFile, String[] autoStartContainers, Set<String> proxiedContainerPorts, String portForwardBindAddress) {
        super(sessionId, masterUrl, namespace, environmentConfigUrl, environmentDependencies, namespaceLazyCreateEnabled, namespaceCleanupEnabled, namespaceCleanupTimeout, namespaceCleanupConfirmationEnabled, namespaceDestroyEnabled, namespaceDestroyConfirmationEnabled, namespaceDestroyTimeout, waitTimeout, waitPollInterval, waitForServiceConnectionEnabled, waitForServiceList, waitForServiceConnectionTimeout, ansiLoggerEnabled, environmentInitEnabled, kubernetesDomain, dockerRegistry);
        this.keepAliveGitServer = keepAliveGitServer;
        this.definitions = definitions;
        this.definitionsFile = definitionsFile;
        this.autoStartContainers = autoStartContainers;
        this.proxiedContainerPorts = proxiedContainerPorts;
        this.portForwardBindAddress = portForwardBindAddress;
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
        if(autoStartContainers == null) {
            return new String[0];
        }
        return autoStartContainers;
    }

    public Set<String> getProxiedContainerPorts() {
        if(proxiedContainerPorts == null) {
            return Collections.emptySet();
        }
        return proxiedContainerPorts;
    }

    public String getPortForwardBindAddress() {
        return portForwardBindAddress;
    }

    private static String[] split(String str, String regex) {
        if (str == null || str.isEmpty()) {
            return new String[0];
        } else {
            return str.split(regex);
        }
    }

    public static CubeOpenShiftConfiguration fromMap(Configuration c, Map<String, String> map) {
        try {
            setDefinitionsFile(c, map);
            return new CubeOpenShiftConfigurationBuilder()
                    .withSessionId(c.getSessionId())
                    .withNamespace(c.getNamespace())
                    .withMasterUrl(c.getMasterUrl())
                    .withEnvironmentInitEnabled(c.isEnvironmentInitEnabled())
                    .withEnvironmentConfigUrl(c.getEnvironmentConfigUrl())
                    .withEnvironmentDependencies(c.getEnvironmentDependencies())
                    .withNamespaceLazyCreateEnabled(c.isNamespaceLazyCreateEnabled())
                    .withNamespaceCleanupEnabled(c.isNamespaceCleanupEnabled())
                    .withNamespaceCleanupConfirmationEnabled(c.isNamespaceCleanupConfirmationEnabled())
                    .withNamespaceCleanupTimeout(c.getNamespaceCleanupTimeout())

                    .withNamespaceDestroyEnabled(c.isNamespaceDestroyEnabled())
                    .withNamespaceDestroyConfirmationEnabled(c.isNamespaceDestroyConfirmationEnabled())
                    .withNamespaceDestroyTimeout(c.getNamespaceDestroyTimeout())

                    .withWaitTimeout(c.getWaitTimeout())
                    .withWaitPollInterval(c.getWaitPollInterval())
                    .withWaitForServiceList(c.getWaitForServiceList())
                    .withWaitForServiceConnectionEnabled(c.isWaitForServiceConnectionEnabled())
                    .withWaitForServiceConnectionTimeout(c.getWaitForServiceConnectionTimeout())
                    .withAnsiLoggerEnabled(c.isAnsiLoggerEnabled())
                    .withKubernetesDomain(c.getKubernetesDomain())
                    .withDockerRegistry(c.getDockerRegistry())
                    //Local properties
                    .withKeepAliveGitServer(getBooleanProperty(KEEP_ALIVE_GIT_SERVER, map, false))
                    .withDefinitions(getStringProperty(DEFINITIONS, map, null))
                    .withDefinitionsFile(getStringProperty(ENV_CONFIG_URL, map, null))
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

    private static void setDefinitionsFile(Configuration c, Map<String, String> map) throws MalformedURLException {
        final String stringProperty = getStringProperty(ENV_CONFIG_URL, map, null);

        if (stringProperty != null) {
            URL configResource = findConfigResource(stringProperty);

            if (configResource == null) {
                final File file = new File(stringProperty);
                if (file.exists()) {
                    configResource = file.toURI().toURL();
                }
            }

            if (c instanceof DefaultConfiguration && configResource != null) {
                DefaultConfiguration defaultConfiguration = (DefaultConfiguration) c;
                defaultConfiguration.setDefinitionsFileURL(configResource);
            }
        }
    }
}
