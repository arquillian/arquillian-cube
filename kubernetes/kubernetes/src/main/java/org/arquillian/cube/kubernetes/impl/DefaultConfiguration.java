package org.arquillian.cube.kubernetes.impl;

import io.fabric8.kubernetes.clnt.v4_0.ConfigBuilder;
import io.fabric8.kubernetes.clnt.v4_0.utils.Utils;
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

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder.v4_0", generateBuilderPackage = false, editableEnabled = false)
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
    private final String logPath;
    private final String kubernetesDomain;
    private final String dockerRegistry;
    private final String username;
    private final String password;
    private final String apiVersion;
    private final boolean trustCerts;

    private final boolean fmpBuildEnabled;
    private final boolean fmpBuildForMavenDisable;
    private final boolean fmpDebugOutput;
    private final boolean fmpLogsEnabled;
    private final String fmpPomPath;
    private final List<String> fmpProfiles;
    private final List<String> fmpSystemProperties;
    private final String fmpBuildOptions;

    private String token;

    public DefaultConfiguration(String sessionId, URL masterUrl, String namespace, Map<String, String> scriptEnvironmentVariables,  URL environmentSetupScriptUrl,
        URL environmentTeardownScriptUrl, URL environmentConfigUrl, List<URL> environmentDependencies, boolean namespaceUseCurrentEnabled,
        boolean namespaceLazyCreateEnabled, boolean namespaceCleanupEnabled, long namespaceCleanupTimeout,
        boolean namespaceCleanupConfirmationEnabled, boolean namespaceDestroyEnabled,
        boolean namespaceDestroyConfirmationEnabled, long namespaceDestroyTimeout, boolean waitEnabled, long waitTimeout,
        long waitPollInterval, List<String> waitForServiceList, boolean ansiLoggerEnabled, boolean environmentInitEnabled, boolean logCopyEnabled,
        String logPath, String kubernetesDomain, String dockerRegistry, String token, String username, String password,
        String apiVersion, boolean trustCerts, boolean fmpBuildEnabled, boolean fmpBuildForMavenDisable,
        boolean fmpDebugOutput, boolean fmpLogsEnabled, String fmpPomPath, List<String> fmpProfiles,
        List<String> fmpSystemProperties, String fmpBuildOptions) {
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
        this.logPath = logPath;
        this.kubernetesDomain = kubernetesDomain;
        this.dockerRegistry = dockerRegistry;
        this.token = token;
        this.username = username;
        this.password = password;
        this.apiVersion = apiVersion;
        this.trustCerts = trustCerts;
        this.fmpBuildEnabled = fmpBuildEnabled;
        this.fmpBuildForMavenDisable = fmpBuildForMavenDisable;
        this.fmpLogsEnabled = fmpLogsEnabled;
        this.fmpDebugOutput = fmpDebugOutput;
        this.fmpPomPath = fmpPomPath;
        this.fmpProfiles = fmpProfiles;
        this.fmpSystemProperties = fmpSystemProperties;
        this.fmpBuildOptions = fmpBuildOptions;
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
                .withFmpBuildEnabled(getBooleanProperty(FMP_BUILD, map, false))
                .withFmpBuildForMavenDisable(getBooleanProperty(FMP_BUILD_DISABLE_FOR_MAVEN, map, false))
                .withFmpDebugOutput(getBooleanProperty(FMP_DEBUG_OUTPUT, map, false))
                .withFmpLogsEnabled(getBooleanProperty(FMP_LOGS, map, true))
                .withFmpPomPath(getStringProperty(FMP_POM_PATH, map, DEFAULT_FMP_PATH))
                .withFmpProfiles(Strings.splitAndTrimAsList(getStringProperty(FMP_PROFILES, map, ""), "\\s*,\\s*"))
                .withFmpSystemProperties(Strings.splitAndTrimAsList(getStringProperty(FMP_SYSTEM_PROPERTIES, map, ""), "\\s*,\\s*"))
                .withFmpBuildOptions(getStringProperty(FMP_BUILD_OPTIONS, map, null))
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
    public String getLogPath() {
        return logPath;
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

    @Override
    public boolean isFmpBuildForMavenDisable() {
        return fmpBuildForMavenDisable;
    }

    @Override
    public boolean isFmpDebugOutput() {
        return fmpDebugOutput;
    }

    @Override
    public boolean isFmpLogsEnabled() {
        return fmpLogsEnabled;
    }

    @Override
    public boolean isFmpBuildEnabled() {
        return fmpBuildEnabled;
    }

    @Override
    public String getFmpPomPath() {
        return fmpPomPath;
    }

    @Override
    public String getFmpBuildOptions() {
        return fmpBuildOptions;
    }

    @Override
    public List<String> getFmpProfiles() {
        return fmpProfiles;
    }

    @Override
    public List<String> getFmpSystemProperties() {
        return fmpSystemProperties;
    }

    protected void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {

        StringBuilder content = new StringBuilder();
        content.append("CubeKubernetesConfiguration: ").append(System.lineSeparator());
        if (namespace != null) {
            appendPropertyWithValue(content, NAMESPACE, namespace);
        }
        if (masterUrl != null) {
            appendPropertyWithValue(content, MASTER_URL, masterUrl);
        }
        if (!scriptEnvironmentVariables.isEmpty()) {
            appendPropertyWithValue(content, ENVIRONMENT_SCRIPT_ENV, scriptEnvironmentVariables);
        }
        if (environmentSetupScriptUrl != null) {
            appendPropertyWithValue(content, ENVIRONMENT_SETUP_SCRIPT_URL, environmentSetupScriptUrl);
        }

        if (environmentTeardownScriptUrl != null) {
            appendPropertyWithValue(content, ENVIRONMENT_TEARDOWN_SCRIPT_URL, environmentTeardownScriptUrl);
        }
        if (environmentConfigUrl != null) {
            appendPropertyWithValue(content, ENVIRONMENT_CONFIG_URL, environmentConfigUrl);
        }
        if (environmentDependencies != null && !environmentDependencies.isEmpty()) {
            appendPropertyWithValue(content, ENVIRONMENT_DEPENDENCIES, environmentDependencies.toString());
        }

        appendPropertyWithValue(content, NAMESPACE_LAZY_CREATE_ENABLED, namespaceLazyCreateEnabled);

        appendPropertyWithValue(content, NAMESPACE_CLEANUP_ENABLED, namespaceCleanupEnabled);
        appendPropertyWithValue(content, NAMESPACE_CLEANUP_TIMEOUT, namespaceCleanupTimeout);
        appendPropertyWithValue(content, NAMESPACE_CLEANUP_CONFIRM_ENABLED, namespaceCleanupConfirmationEnabled);

        appendPropertyWithValue(content, NAMESPACE_DESTROY_ENABLED, namespaceDestroyEnabled);
        appendPropertyWithValue(content, NAMESPACE_DESTROY_CONFIRM_ENABLED, namespaceDestroyConfirmationEnabled);
        appendPropertyWithValue(content, NAMESPACE_DESTROY_TIMEOUT, namespaceDestroyTimeout);

        appendPropertyWithValue(content, WAIT_ENABLED, waitEnabled);
        appendPropertyWithValue(content, WAIT_TIMEOUT, waitTimeout);
        appendPropertyWithValue(content, WAIT_POLL_INTERVAL, waitPollInterval);

        appendPropertyWithValue(content, ANSI_LOGGER_ENABLED, ansiLoggerEnabled);
        appendPropertyWithValue(content, ENVIRONMENT_INIT_ENABLED, environmentInitEnabled);

        appendPropertyWithValue(content, LOGS_COPY, logCopyEnabled);

        if (!waitForServiceList.isEmpty()) {
            appendPropertyWithValue(content, WAIT_FOR_SERVICE_LIST, waitForServiceList);
        }
        if (logPath != null) {
            appendPropertyWithValue(content, LOGS_PATH, logPath);
        }
        if (kubernetesDomain != null) {
            appendPropertyWithValue(content, KUBERNETES_DOMAIN, kubernetesDomain);
        }
        if (dockerRegistry != null) {
            appendPropertyWithValue(content, DOCKER_REGISTY, dockerRegistry);
        }
        if (apiVersion != null) {
            appendPropertyWithValue(content, API_VERSION, apiVersion);
        }
        if (username != null) {
            appendPropertyWithValue(content, USERNAME, username);
        }
        if (password != null) {
            appendPropertyWithValue(content, PASSWORD, password);
        }
        if (token != null) {
            appendPropertyWithValue(content, AUTH_TOKEN, token);
        }

        appendPropertyWithValue(content, TRUST_CERTS, trustCerts);

        appendPropertyWithValue(content, FMP_BUILD, fmpBuildEnabled);
        appendPropertyWithValue(content, FMP_BUILD_DISABLE_FOR_MAVEN, fmpBuildForMavenDisable);
        appendPropertyWithValue(content, FMP_POM_PATH, fmpPomPath);
        appendPropertyWithValue(content, FMP_DEBUG_OUTPUT, fmpDebugOutput);
        appendPropertyWithValue(content, FMP_LOGS, fmpLogsEnabled);
        if (!fmpProfiles.isEmpty()) {
            appendPropertyWithValue(content, FMP_PROFILES, fmpProfiles);
        }

        if (!fmpSystemProperties.isEmpty()) {
            appendPropertyWithValue(content, FMP_SYSTEM_PROPERTIES, fmpSystemProperties);
        }

        if (fmpBuildOptions != null) {
            appendPropertyWithValue(content, FMP_BUILD_OPTIONS, fmpBuildOptions);
        }

        return content.toString();
    }

    protected void appendPropertyWithValue(StringBuilder content, String property, Object value) {
        content.append("  ").append(property).append(" = ").append(value).append(System.lineSeparator());
    }
}
