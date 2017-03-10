package org.arquillian.cube.kubernetes.impl;

import io.fabric8.kubernetes.client.utils.Utils;
import io.sundr.builder.annotations.Buildable;
import org.arquillian.cube.impl.util.Strings;
import org.arquillian.cube.impl.util.SystemEnvironmentVariables;
import org.arquillian.cube.kubernetes.api.Configuration;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.arquillian.cube.impl.util.ConfigUtil.asURL;
import static org.arquillian.cube.impl.util.ConfigUtil.getBooleanProperty;
import static org.arquillian.cube.impl.util.ConfigUtil.getLongProperty;
import static org.arquillian.cube.impl.util.ConfigUtil.getStringProperty;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder", generateBuilderPackage = false, editableEnabled = false)
public class DefaultConfiguration implements Configuration {

    private final String sessionId;
    private final String namespace;
    private final URL masterUrl;

    private final URL environmentSetupScriptUrl;
    private final URL environmentTeardownScriptUrl;

    private final URL environmentConfigUrl;
    private final List<URL> environmentDependencies;

    private final boolean namespaceLazyCreateEnabled;
    private final boolean namespaceCleanupEnabled;
    private final long namespaceCleanupTimeout;
    private final boolean namespaceCleanupConfirmationEnabled;

    private final boolean namespaceDestroyEnabled;
    private final boolean namespaceDestroyConfirmationEnabled;
    private final long namespaceDestroyTimeout;

    private final long waitTimeout;
    private final long waitPollInterval;
    private final boolean waitForServiceConnectionEnabled;
    private final List<String> waitForServiceList;
    private final long waitForServiceConnectionTimeout;

    private final boolean ansiLoggerEnabled;
    private final boolean environmentInitEnabled;
    private final String kubernetesDomain;
    private final String dockerRegistry;


    public static DefaultConfiguration fromMap(Map<String, String> map) {
        try {
            String sessionId = UUID.randomUUID().toString();
            String namespace = getStringProperty(NAMESPACE_TO_USE, map, null);
            if (Strings.isNullOrEmpty(namespace)) {
                namespace = getStringProperty(NAMESPACE_PREFIX, map, "itest") + "-" + sessionId;
            }
            return new DefaultConfigurationBuilder()
                    .withSessionId(sessionId)
                    .withNamespace(namespace)
                    .withMasterUrl(new URL(getStringProperty(MASTER_URL, KUBERNETES_MASTER, map, FALLBACK_CLIENT_CONFIG.getMasterUrl())))
                    .withEnvironmentInitEnabled(getBooleanProperty(ENVIRONMENT_INIT_ENABLED, map, true))
                    .withEnvironmentSetupScriptUrl(asUrlOrResource(getStringProperty(ENVIRONMENT_SETUP_SCRIPT_URL, map, null)))
                    .withEnvironmentTeardownScriptUrl(asUrlOrResource(getStringProperty(ENVIRONMENT_TEARDOWN_SCRIPT_URL, map, null)))
                    .withEnvironmentConfigUrl(getKubernetesConfigurationUrl(map))
                    .withEnvironmentDependencies(asURL(Strings.splitAndTrimAsList(getStringProperty(ENVIRONMENT_DEPENDENCIES, map, ""), " ")))
                    .withNamespaceLazyCreateEnabled(getBooleanProperty(NAMESPACE_LAZY_CREATE_ENABLED, map, DEFAULT_NAMESPACE_LAZY_CREATE_ENABLED))
                    .withNamespaceCleanupEnabled(getBooleanProperty(NAMESPACE_CLEANUP_ENABLED, map, namespace.contains(sessionId)))
                    .withNamespaceCleanupConfirmationEnabled(getBooleanProperty(NAMESPACE_CLEANUP_CONFIRM_ENABLED, map, false))
                    .withNamespaceCleanupTimeout(getLongProperty(NAMESPACE_CLEANUP_TIMEOUT, map, DEFAULT_NAMESPACE_CLEANUP_TIMEOUT))

                    .withNamespaceDestroyEnabled(getBooleanProperty(NAMESPACE_DESTROY_ENABLED, map, namespace.contains(sessionId)))
                    .withNamespaceDestroyConfirmationEnabled(getBooleanProperty(NAMESPACE_DESTROY_CONFIRM_ENABLED, map, false))
                    .withNamespaceDestroyTimeout(getLongProperty(NAMESPACE_DESTROY_TIMEOUT, map, DEFAULT_NAMESPACE_DESTROY_TIMEOUT))

                    .withWaitTimeout(getLongProperty(WAIT_TIMEOUT, map, DEFAULT_WAIT_TIMEOUT))
                    .withWaitPollInterval(getLongProperty(WAIT_POLL_INTERVAL, map, DEFAULT_WAIT_POLL_INTERVAL))
                    .withWaitForServiceList(Strings.splitAndTrimAsList(getStringProperty(WAIT_FOR_SERVICE_LIST, map, ""), " "))
                    .withWaitForServiceConnectionEnabled(getBooleanProperty(WAIT_FOR_SERVICE_CONNECTION_ENABLED, map, DEFAULT_WAIT_FOR_SERVICE_CONNECTION_ENABLED))
                    .withWaitForServiceConnectionTimeout(getLongProperty(WAIT_FOR_SERVICE_CONNECTION_TIMEOUT, map, DEFAULT_WAIT_FOR_SERVICE_CONNECTION_TIMEOUT))
                    .withAnsiLoggerEnabled(getBooleanProperty(ANSI_LOGGER_ENABLED, map, true))
                    .withKubernetesDomain(getStringProperty(DOMAIN, KUBERNETES_DOMAIN, map, null))
                    .withDockerRegistry(getDockerRegistry(map))
                    .build();
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                throw new RuntimeException(t);
            }
        }
    }


    public DefaultConfiguration(String sessionId, URL masterUrl, String namespace, URL environmentSetupScriptUrl, URL environmentTeardownScriptUrl, URL environmentConfigUrl, List<URL> environmentDependencies, boolean namespaceLazyCreateEnabled, boolean namespaceCleanupEnabled, long namespaceCleanupTimeout, boolean namespaceCleanupConfirmationEnabled, boolean namespaceDestroyEnabled, boolean namespaceDestroyConfirmationEnabled, long namespaceDestroyTimeout, long waitTimeout, long waitPollInterval, boolean waitForServiceConnectionEnabled, List<String> waitForServiceList, long waitForServiceConnectionTimeout, boolean ansiLoggerEnabled, boolean environmentInitEnabled, String kubernetesDomain, String dockerRegistry) {
        this.masterUrl = masterUrl;
        this.environmentSetupScriptUrl = environmentSetupScriptUrl;
        this.environmentTeardownScriptUrl = environmentTeardownScriptUrl;
        this.environmentDependencies = environmentDependencies;
        this.environmentConfigUrl = environmentConfigUrl;
        this.sessionId = sessionId;
        this.namespace = namespace;
        this.namespaceLazyCreateEnabled = namespaceLazyCreateEnabled;
        this.namespaceCleanupEnabled = namespaceCleanupEnabled;
        this.namespaceCleanupTimeout = namespaceCleanupTimeout;
        this.namespaceCleanupConfirmationEnabled = namespaceCleanupConfirmationEnabled;
        this.namespaceDestroyEnabled = namespaceDestroyEnabled;
        this.namespaceDestroyConfirmationEnabled = namespaceDestroyConfirmationEnabled;
        this.namespaceDestroyTimeout = namespaceDestroyTimeout;
        this.waitTimeout = waitTimeout;
        this.waitPollInterval = waitPollInterval;
        this.waitForServiceConnectionEnabled = waitForServiceConnectionEnabled;
        this.waitForServiceList = waitForServiceList;
        this.waitForServiceConnectionTimeout = waitForServiceConnectionTimeout;
        this.ansiLoggerEnabled = ansiLoggerEnabled;
        this.environmentInitEnabled = environmentInitEnabled;
        this.kubernetesDomain = kubernetesDomain;
        this.dockerRegistry = dockerRegistry;
    }


    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public URL getMasterUrl() {
        return masterUrl;
    }

    public URL getEnvironmentSetupScriptUrl() {
        return environmentSetupScriptUrl;
    }

    @Override
    public URL getEnvironmentTeardownScriptUrl() {
        return environmentTeardownScriptUrl;
    }

    @Override
    public URL getEnvironmentConfigUrl() {
        return environmentConfigUrl;
    }

    @Override
    public List<URL> getEnvironmentDependencies() {
        return environmentDependencies;
    }

    @Override
    public boolean isNamespaceLazyCreateEnabled() {
        return namespaceLazyCreateEnabled;
    }

    @Override
    public boolean isNamespaceCleanupEnabled() {
        return namespaceCleanupEnabled;
    }

    @Override
    public long getNamespaceCleanupTimeout() {
        return namespaceCleanupTimeout;
    }

    @Override
    public boolean isNamespaceCleanupConfirmationEnabled() {
        return namespaceCleanupConfirmationEnabled;
    }

    @Override
    public boolean isNamespaceDestroyEnabled() {
        return namespaceDestroyEnabled;
    }

    @Override
    public boolean isNamespaceDestroyConfirmationEnabled() {
        return namespaceDestroyConfirmationEnabled;
    }

    @Override
    public long getNamespaceDestroyTimeout() {
        return namespaceDestroyTimeout;
    }

    @Override
    public long getWaitTimeout() {
        return waitTimeout;
    }

    @Override
    public long getWaitPollInterval() {
        return waitPollInterval;
    }

    @Override
    public boolean isWaitForServiceConnectionEnabled() {
        return waitForServiceConnectionEnabled;
    }

    @Override
    public List<String> getWaitForServiceList() {
        return waitForServiceList;
    }

    @Override
    public long getWaitForServiceConnectionTimeout() {
        return waitForServiceConnectionTimeout;
    }

    @Override
    public boolean isAnsiLoggerEnabled() {
        return ansiLoggerEnabled;
    }

    @Override
    public boolean isEnvironmentInitEnabled() {
        return environmentInitEnabled;
    }

    @Override
    public String getKubernetesDomain() {
        return kubernetesDomain;
    }

    @Override
    public String getDockerRegistry() {
        return dockerRegistry;
    }

    public static String getDockerRegistry(Map<String, String> map) throws MalformedURLException {
        if (map.containsKey(DOCKER_REGISTY)) {
            return map.get(DOCKER_REGISTY);
        }

        String registry = SystemEnvironmentVariables.getEnvironmentOrPropertyVariable(DOCKER_REGISTY);
        if (Strings.isNotNullOrEmpty(registry)) {
            return registry;
        }

        String registryHost = SystemEnvironmentVariables.getEnvironmentVariable(DOCKER_REGISTRY_HOST);
        String registryPort = SystemEnvironmentVariables.getEnvironmentVariable(DOCKER_REGISTRY_PORT);
        if (Strings.isNotNullOrEmpty(registry) && Strings.isNotNullOrEmpty(registryPort)) {
            return String.format(DOCKER_REGISTRY_FORMAT, registryHost, registryPort);
        } else {
            return null;
        }
    }

    /**
     * Applies the kubernetes json url to the configuration.
     *
     * @param map The arquillian configuration.
     */
    public static URL getKubernetesConfigurationUrl(Map<String, String> map) throws MalformedURLException {
        if (map.containsKey(ENVIRONMENT_CONFIG_URL)) {
            return new URL(map.get(ENVIRONMENT_CONFIG_URL));
        } else if (map.containsKey(ENVIRONMENT_CONFIG_RESOURCE_NAME)) {
            String resourceName = map.get(ENVIRONMENT_CONFIG_RESOURCE_NAME);
            return findConfigResource(resourceName);
        } else if (Strings.isNotNullOrEmpty(Utils.getSystemPropertyOrEnvVar(ENVIRONMENT_CONFIG_URL, ""))) {
            return new URL(Utils.getSystemPropertyOrEnvVar(ENVIRONMENT_CONFIG_URL, ""));
        } else {
            String defaultValue = "/" + DEFAULT_CONFIG_FILE_NAME;
            String resourceName = Utils.getSystemPropertyOrEnvVar(ENVIRONMENT_CONFIG_RESOURCE_NAME, defaultValue);
            URL answer = findConfigResource(resourceName);
            if (answer == null) {
            }
            return answer;
        }
    }

    /**
     * Returns the URL of a classpath resource.
     * @param resourceName  The name of the resource.
     * @return              The URL.
     */
    public static URL findConfigResource(String resourceName) {
        if (Strings.isNullOrEmpty(resourceName)) {
            return null;
        }
        return resourceName.startsWith("/") ? DefaultConfiguration.class.getResource(resourceName) : DefaultConfiguration.class.getResource("/" + resourceName);
    }


    /**
     * Convert a string to a URL and fallback to classpath resource, if not convertible.
     * @param s The string to convert.
     * @return  The URL.
     */
    public static URL asUrlOrResource(String s) {
        if (Strings.isNullOrEmpty(s)) {
            return null;
        }

        try {
            return new URL(s);
        } catch (MalformedURLException e) {
            //If its not a valid URL try to treat it as a local resource.
            return findConfigResource(s);
        }
    }
}
