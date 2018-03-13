package org.arquillian.cube.kubernetes.impl;

import io.fabric8.kubernetes.clnt.v3_1.ConfigBuilder;
import io.fabric8.kubernetes.clnt.v3_1.utils.Utils;
import io.sundr.builder.annotations.Buildable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;
import org.arquillian.cube.impl.util.Strings;
import org.arquillian.cube.impl.util.SystemEnvironmentVariables;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.api.KubernetesResourceLocator;

import static org.arquillian.cube.impl.util.ConfigUtil.asURL;
import static org.arquillian.cube.impl.util.ConfigUtil.getBooleanProperty;
import static org.arquillian.cube.impl.util.ConfigUtil.getLongProperty;
import static org.arquillian.cube.impl.util.ConfigUtil.getStringProperty;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder.v3_1", generateBuilderPackage = false, editableEnabled = false)
public class DefaultConfiguration implements Configuration {

    private static final String ENV_VAR_REGEX = "env.([a-zA-Z0-9_]+)";
    private static final Pattern ENV_VAR_PATTERN = Pattern.compile(ENV_VAR_REGEX);

    private static final String ROOT = "/";

    private final String sessionId;
    private final String namespace;
    private final URL masterUrl;

    private final Map<String, String> scriptEnvironmentVariables;
    private final URL environmentSetupScriptUrl;
    private final URL environmentTeardownScriptUrl;

    private final URL environmentConfigUrl;
    private final List<URL> environmentDependencies;

    private final boolean namespaceLazyCreateEnabled;
    private final boolean namespaceCleanupEnabled;
    private final long namespaceCleanupTimeout;
    private final boolean namespaceCleanupConfirmationEnabled;
    private final boolean namespaceUseCurrentEnabled;

    private final boolean namespaceDestroyEnabled;
    private final boolean namespaceDestroyConfirmationEnabled;
    private final long namespaceDestroyTimeout;

    private final boolean waitEnabled;
    private final long waitTimeout;
    private final long waitPollInterval;
    private final List<String> waitForServiceList;

    private final boolean ansiLoggerEnabled;
    private final boolean environmentInitEnabled;
    private final boolean logCopyEnabled;
    private final boolean fmpBuildEnabled;
    private final String fmpPath;
    private final String logPath;
    private final String kubernetesDomain;
    private final String dockerRegistry;
    private final String username;
    private final String password;
    private final String apiVersion;
    private final boolean trustCerts;

    private String token;

    public DefaultConfiguration(String sessionId, URL masterUrl, String namespace, Map<String, String> scriptEnvironmentVariables,  URL environmentSetupScriptUrl,
        URL environmentTeardownScriptUrl, URL environmentConfigUrl, List<URL> environmentDependencies, boolean namespaceUseCurrentEnabled,
        boolean namespaceLazyCreateEnabled, boolean namespaceCleanupEnabled, long namespaceCleanupTimeout,
        boolean namespaceCleanupConfirmationEnabled, boolean namespaceDestroyEnabled,
        boolean namespaceDestroyConfirmationEnabled, long namespaceDestroyTimeout, boolean waitEnabled, long waitTimeout,
        long waitPollInterval, List<String> waitForServiceList, boolean ansiLoggerEnabled, boolean environmentInitEnabled, boolean logCopyEnabled, boolean fmpBuildEnabled,
        String fmpPath, String logPath, String kubernetesDomain, String dockerRegistry, String token, String username, String password,
        String apiVersion, boolean trustCerts) {
        this.masterUrl = masterUrl;
        this.scriptEnvironmentVariables = scriptEnvironmentVariables;
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
        this.namespaceUseCurrentEnabled = namespaceUseCurrentEnabled;
        this.waitEnabled = waitEnabled;
        this.waitTimeout = waitTimeout;
        this.waitPollInterval = waitPollInterval;
        this.waitForServiceList = waitForServiceList;
        this.ansiLoggerEnabled = ansiLoggerEnabled;
        this.environmentInitEnabled = environmentInitEnabled;
        this.logCopyEnabled = logCopyEnabled;
        this.fmpBuildEnabled = fmpBuildEnabled;
        this.logPath = logPath;
        this.fmpPath = fmpPath;
        this.kubernetesDomain = kubernetesDomain;
        this.dockerRegistry = dockerRegistry;
        this.token = token;
        this.username = username;
        this.password = password;
        this.apiVersion = apiVersion;
        this.trustCerts = trustCerts;
    }

    public static DefaultConfiguration fromMap(Map<String, String> map) {
        try {
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
            return new DefaultConfigurationBuilder()
                .withSessionId(sessionId)
                .withNamespace(namespace)
                .withMasterUrl(
                    new URL(getStringProperty(MASTER_URL, KUBERNETES_MASTER, map, FALLBACK_CLIENT_CONFIG.getMasterUrl())))
                .withEnvironmentInitEnabled(getBooleanProperty(ENVIRONMENT_INIT_ENABLED, map, true))
                .withLogCopyEnabled(getBooleanProperty(LOGS_COPY, map, false))
                .withFmpBuildEnabled(getBooleanProperty(FMP_BUILD, map, false))
                .withFmpPath(getStringProperty(FMP_PATH, map, DEFAULT_FMP_PATH))
                .withFmpBuildEnabled(getBooleanProperty(FMP_BUILD, map, false))
                .withLogPath(getStringProperty(LOGS_PATH, map, null))
                .withScriptEnvironmentVariables(parseMap(map.get(ENVIRONMENT_SCRIPT_ENV)))
                .withEnvironmentSetupScriptUrl(
                    asUrlOrResource(getStringProperty(ENVIRONMENT_SETUP_SCRIPT_URL, map, null)))
                .withEnvironmentTeardownScriptUrl(
                    asUrlOrResource(getStringProperty(ENVIRONMENT_TEARDOWN_SCRIPT_URL, map, null)))
                .withEnvironmentConfigUrl(getKubernetesConfigurationUrl(map))
                .withEnvironmentDependencies(
                    asURL(Strings.splitAndTrimAsList(getStringProperty(ENVIRONMENT_DEPENDENCIES, map, ""), "\\s*,\\s*")))
                .withNamespaceLazyCreateEnabled(
                    getBooleanProperty(NAMESPACE_LAZY_CREATE_ENABLED, map, DEFAULT_NAMESPACE_LAZY_CREATE_ENABLED))
                .withNamespaceCleanupEnabled(getBooleanProperty(NAMESPACE_CLEANUP_ENABLED, map, true))
                .withNamespaceCleanupConfirmationEnabled(
                    getBooleanProperty(NAMESPACE_CLEANUP_CONFIRM_ENABLED, map, false))
                .withNamespaceCleanupTimeout(
                    getLongProperty(NAMESPACE_CLEANUP_TIMEOUT, map, DEFAULT_NAMESPACE_CLEANUP_TIMEOUT))
                .withNamespaceUseCurrentEnabled(getBooleanProperty(NAMESPACE_USE_CURRENT, map, false))

                .withNamespaceDestroyEnabled(getBooleanProperty(NAMESPACE_DESTROY_ENABLED, map, shouldDestroyNamespace))
                .withNamespaceDestroyConfirmationEnabled(
                    getBooleanProperty(NAMESPACE_DESTROY_CONFIRM_ENABLED, map, false))
                .withNamespaceDestroyTimeout(
                    getLongProperty(NAMESPACE_DESTROY_TIMEOUT, map, DEFAULT_NAMESPACE_DESTROY_TIMEOUT))

                .withWaitEnabled(getBooleanProperty(WAIT_ENABLED, map, true))
                .withWaitTimeout(getLongProperty(WAIT_TIMEOUT, map, DEFAULT_WAIT_TIMEOUT))
                .withWaitPollInterval(getLongProperty(WAIT_POLL_INTERVAL, map, DEFAULT_WAIT_POLL_INTERVAL))
                .withWaitForServiceList(
                    Strings.splitAndTrimAsList(getStringProperty(WAIT_FOR_SERVICE_LIST, map, ""), "\\s*,\\s*"))
                .withAnsiLoggerEnabled(getBooleanProperty(ANSI_LOGGER_ENABLED, map, true))
                .withKubernetesDomain(getStringProperty(DOMAIN, KUBERNETES_DOMAIN, map, null))
                .withDockerRegistry(getDockerRegistry(map))
                .withToken(getStringProperty(AUTH_TOKEN, map, null))
                .withUsername(getStringProperty(USERNAME, map, null))
                .withPassword(getStringProperty(PASSWORD, map, null))
                .withApiVersion(getStringProperty(API_VERSION, map, "v1"))
                .withTrustCerts(getBooleanProperty(TRUST_CERTS, map, true))
                .build();
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                throw new RuntimeException(t);
            }
        }
    }

    public static String getDockerRegistry(Map<String, String> map) throws MalformedURLException {
        String registry = SystemEnvironmentVariables.getEnvironmentOrPropertyVariable(DOCKER_REGISTY);
        if (Strings.isNotNullOrEmpty(registry)) {
            return registry;
        }

        String registryHost = SystemEnvironmentVariables.getEnvironmentVariable(DOCKER_REGISTRY_HOST);
        String registryPort = SystemEnvironmentVariables.getEnvironmentVariable(DOCKER_REGISTRY_PORT);
        if (Strings.isNotNullOrEmpty(registry) && Strings.isNotNullOrEmpty(registryPort)) {
            return String.format(DOCKER_REGISTRY_FORMAT, registryHost, registryPort);
        }

        if (map.containsKey(DOCKER_REGISTY)) {
            return map.get(DOCKER_REGISTY);
        }

        return null;
    }

    /**
     * Applies the kubernetes json url to the configuration.
     *
     * @param map
     *     The arquillian configuration.
     */
    public static URL getKubernetesConfigurationUrl(Map<String, String> map) throws MalformedURLException {
        if (Strings.isNotNullOrEmpty(Utils.getSystemPropertyOrEnvVar(ENVIRONMENT_CONFIG_URL, ""))) {
            return new URL(Utils.getSystemPropertyOrEnvVar(ENVIRONMENT_CONFIG_URL, ""));
        } else if (Strings.isNotNullOrEmpty(Utils.getSystemPropertyOrEnvVar(ENVIRONMENT_CONFIG_RESOURCE_NAME, ""))) {
            String resourceName = Utils.getSystemPropertyOrEnvVar(ENVIRONMENT_CONFIG_RESOURCE_NAME, "");
            return findConfigResource(resourceName);
        } else if (map.containsKey(ENVIRONMENT_CONFIG_URL)) {
            return new URL(map.get(ENVIRONMENT_CONFIG_URL));
        } else if (map.containsKey(ENVIRONMENT_CONFIG_RESOURCE_NAME)) {
            String resourceName = map.get(ENVIRONMENT_CONFIG_RESOURCE_NAME);
            return findConfigResource(resourceName);
        } else {
            // Let the resource locator find the resource
            return null;
        }
    }

    /**
     * Returns the URL of a classpath resource.
     *
     * @param resourceName
     *     The name of the resource.
     *
     * @return The URL.
     */
    public static URL findConfigResource(String resourceName) {
        if (Strings.isNullOrEmpty(resourceName)) {
            return null;
        }

        final URL url = resourceName.startsWith(ROOT) ? DefaultConfiguration.class.getResource(resourceName)
            : DefaultConfiguration.class.getResource(ROOT + resourceName);

        if (url != null) {
            return url;
        }

        // This is useful to get resource under META-INF directory
        String[] resourceNamePrefix = new String[] {"META-INF/fabric8/", "META-INF/fabric8/"};

        for (String resource : resourceNamePrefix) {
            String fullResourceName = resource + resourceName;

            URL candidate = KubernetesResourceLocator.class.getResource(fullResourceName.startsWith(ROOT) ? fullResourceName : ROOT + fullResourceName);
            if (candidate != null) {
                return candidate;
            }
        }

        return null;
    }

    /**
     * Convert a string to a URL and fallback to classpath resource, if not convertible.
     *
     * @param s
     *     The string to convert.
     *
     * @return The URL.
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

    public static Map<String, String> parseMap(String s) throws IOException {
        if (Strings.isNullOrEmpty(s)) {
            return Collections.EMPTY_MAP;
        }

        Properties properties = new Properties();
        try (InputStream is = new ByteArrayInputStream(s.getBytes(Charset.defaultCharset()))) {
            properties.load(is);
        }
        Map<String, String> map = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            map.put(key, properties.getProperty(key));
        }
        return map;
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

    @Override
    public Map<String, String> getScriptEnvironmentVariables() {
        return scriptEnvironmentVariables;
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
    public boolean isNamespaceUseCurrentEnabled() {
        return namespaceUseCurrentEnabled;
    }

    @Override
    public long getNamespaceDestroyTimeout() {
        return namespaceDestroyTimeout;
    }

    @Override
    public boolean isWaitEnabled() {
        return waitEnabled;
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
    public List<String> getWaitForServiceList() {
        return waitForServiceList;
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
    public boolean isLogCopyEnabled() {
        return logCopyEnabled;
    }

    @Override
    public boolean isFmpBuildEnabled() {
        return fmpBuildEnabled;
    }

    @Override
    public String getLogPath() {
        return logPath;
    }

    @Override
    public String getFmpPath() {
        return fmpPath;
    }

    @Override
    public String getKubernetesDomain() {
        return kubernetesDomain;
    }

    @Override
    public String getDockerRegistry() {
        return dockerRegistry;
    }

    public boolean hasBasicAuth() {
        return Strings.isNotNullOrEmpty(username) && Strings.isNotNullOrEmpty(password);
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String getApiVersion() {
        return apiVersion;
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public boolean isTrustCerts() {
        return trustCerts;
    }

    protected void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {

        String lineSeparator = System.lineSeparator();
        StringBuilder content = new StringBuilder();

        content.append("CubeKubernetesConfiguration: ").append(lineSeparator);
        if (namespace != null) {
            content.append("  ").append(NAMESPACE).append(" = ").append(namespace).append(lineSeparator);
        }
        if (masterUrl != null) {
            content.append("  ").append(MASTER_URL).append(" = ").append(masterUrl).append(lineSeparator);
        }
        if (scriptEnvironmentVariables != null) {
            content.append("  ").append(ENVIRONMENT_SCRIPT_ENV).append(" = ").append(scriptEnvironmentVariables).append(lineSeparator);
        }
        if (environmentSetupScriptUrl != null) {
            content.append("  ").append(ENVIRONMENT_SETUP_SCRIPT_URL).append(" = ").append(environmentSetupScriptUrl).append(lineSeparator);
        }

        if (environmentTeardownScriptUrl != null) {
            content.append("  ").append(ENVIRONMENT_TEARDOWN_SCRIPT_URL).append(" = ").append(environmentTeardownScriptUrl).append(lineSeparator);
        }
        if (environmentConfigUrl != null) {
            content.append("  ").append(ENVIRONMENT_CONFIG_URL).append(" = ").append(environmentConfigUrl).append(lineSeparator);
        }
        if (environmentDependencies != null) {
            content.append("  ").append(ENVIRONMENT_DEPENDENCIES).append(" = ").append(environmentDependencies).append(lineSeparator);
        }

        content.append("  ").append(NAMESPACE_LAZY_CREATE_ENABLED).append(" = ").append(namespaceLazyCreateEnabled).append(lineSeparator);

        content.append("  ").append(NAMESPACE_CLEANUP_ENABLED).append(" = ").append(namespaceCleanupEnabled).append(lineSeparator);
        content.append("  ").append(NAMESPACE_CLEANUP_TIMEOUT).append(" = ").append(namespaceCleanupTimeout).append(lineSeparator);
        content.append("  ").append(NAMESPACE_CLEANUP_CONFIRM_ENABLED).append(" = ").append(namespaceCleanupConfirmationEnabled).append(lineSeparator);

        content.append("  ").append(NAMESPACE_DESTROY_ENABLED).append(" = ").append(namespaceDestroyEnabled).append(lineSeparator);
        content.append("  ").append(NAMESPACE_DESTROY_CONFIRM_ENABLED).append(" = ").append(namespaceDestroyConfirmationEnabled).append(lineSeparator);
        content.append("  ").append(NAMESPACE_DESTROY_TIMEOUT).append(" = ").append(namespaceDestroyTimeout).append(lineSeparator);

        content.append("  ").append(WAIT_ENABLED).append(" = ").append(waitEnabled).append(lineSeparator);
        content.append("  ").append(WAIT_TIMEOUT).append(" = ").append(waitTimeout).append(lineSeparator);
        content.append("  ").append(WAIT_POLL_INTERVAL).append(" = ").append(waitPollInterval).append(lineSeparator);

        content.append("  ").append(ANSI_LOGGER_ENABLED).append(" = ").append(ansiLoggerEnabled).append(lineSeparator);
        content.append("  ").append(ENVIRONMENT_INIT_ENABLED).append(" = ").append(environmentInitEnabled).append(lineSeparator);
        content.append("  ").append(LOGS_COPY).append(" = ").append(logCopyEnabled).append(lineSeparator);


        if (waitForServiceList != null) {
            content.append("  ").append(WAIT_FOR_SERVICE_LIST).append(" = ").append(waitForServiceList).append(lineSeparator);
        }
        if (logPath != null) {
            content.append("  ").append(LOGS_PATH).append(" = ").append(logPath).append(lineSeparator);
        }
        if (kubernetesDomain != null) {
            content.append("  ").append(KUBERNETES_DOMAIN).append(" = ").append(kubernetesDomain).append(lineSeparator);

        }

        if (dockerRegistry != null) {
            content.append("  ").append(DOCKER_REGISTY).append(" = ").append(dockerRegistry).append(lineSeparator);
        }
        if (apiVersion != null) {
            content.append("  ").append(API_VERSION).append(" = ").append(apiVersion).append(lineSeparator);
        }

        if (username != null) {
            content.append("  ").append(USERNAME).append(" = ").append(username).append(lineSeparator);
        }
        if (password != null) {
            content.append("  ").append(PASSWORD).append(" = ").append(password).append(lineSeparator);
        }

        if (token != null) {
            content.append("  ").append(AUTH_TOKEN).append(" = ").append(token).append(lineSeparator);
        }
        content.append("  ").append(TRUST_CERTS).append(" = ").append(trustCerts).append(lineSeparator);

        return content.toString();
    }
}
