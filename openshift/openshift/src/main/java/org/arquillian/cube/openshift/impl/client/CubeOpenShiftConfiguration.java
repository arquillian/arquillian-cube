package org.arquillian.cube.openshift.impl.client;

import io.fabric8.kubernetes.clnt.v3_1.Config;
import io.fabric8.kubernetes.clnt.v3_1.ConfigBuilder;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import java.io.Serializable;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import org.arquillian.cube.impl.util.Strings;
import org.arquillian.cube.kubernetes.impl.DefaultConfiguration;
import org.arquillian.cube.openshift.api.ConfigurationHandle;

import static org.arquillian.cube.impl.util.ConfigUtil.asURL;
import static org.arquillian.cube.impl.util.ConfigUtil.getBooleanProperty;
import static org.arquillian.cube.impl.util.ConfigUtil.getIntProperty;
import static org.arquillian.cube.impl.util.ConfigUtil.getLongProperty;
import static org.arquillian.cube.impl.util.ConfigUtil.getStringProperty;
import static org.arquillian.cube.openshift.impl.utils.Strings.isNotNullOrEmpty;
import static org.arquillian.cube.openshift.impl.utils.Strings.isNullOrEmpty;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder.v3_1", generateBuilderPackage = false, editableEnabled = false, refs = {
    @BuildableReference(DefaultConfiguration.class)
})
public class CubeOpenShiftConfiguration extends DefaultConfiguration implements
    ConfigurationHandle, Serializable{

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
    private static final String AUTH_TOKEN = "authToken";
    private static final String ROUTER_SNI_PORT = "routerSniPort";
    private static final String TEMPLATE_URL = "templateUrl";
    private static final String TEMPLATE_LABELS = "templateLabels";
    private static final String TEMPLATE_PARAMETERS = "templateParameters";
    private static final String TEMPLATE_PROCESS = "templateProcess";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String API_VERSION = "apiVersion";
    private static final String TRUST_CERTS = "trustCerts";
    private static final String STARTUP_TIMEOUT = "stratupTimeout";
    private static final String HTTP_CLIENT_TIMEOUT = "httpClientTimeout";


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
    private String token;
    private final int routerSniPort;
    private final String templateURL;
    private final String templateLabels;
    private final String templateParameters;
    private final boolean templateProcess;
    private final String username;
    private final String password;
    private final String apiVersion;
    private final boolean trustCerts;
    private final long startupTimeout;
    private final long httpClientTimeout;


    private OpenShiftClient client;

    public CubeOpenShiftConfiguration(String sessionId, URL masterUrl, String namespace, Map<String, String> scriptEnvironmentVariables, URL environmentSetupScriptUrl,
                                      URL environmentTeardownScriptUrl, URL environmentConfigUrl, List<URL> environmentDependencies,
                                      boolean namespaceLazyCreateEnabled, boolean namespaceCleanupEnabled, long namespaceCleanupTimeout,
                                      boolean namespaceCleanupConfirmationEnabled, boolean namespaceDestroyEnabled, long namespaceDestroyTimeout,
                                      boolean namespaceDestroyConfirmationEnabled, boolean waitEnabled, long waitTimeout, long waitPollInterval,
                                      List<String> waitForServiceList, boolean ansiLoggerEnabled, boolean environmentInitEnabled, boolean logCopyEnabled,
                                      String logPath, String kubernetesDomain, String dockerRegistry, boolean keepAliveGitServer, String definitions,
                                      String definitionsFile, String[] autoStartContainers, Set<String> proxiedContainerPorts,
                                      String portForwardBindAddress, String routerHost, int openshiftRouterHttpPort, int openshiftRouterHttpsPort, boolean enableImageStreamDetection,
                                      String token, int routerSniPort, String templateURL, String templateLabels, String templateParameters, boolean templateProcess,
                                      String username, String password, String apiVersion, boolean trustCerts, long startupTimeout, long httpClientTimeout) {
        super(sessionId, masterUrl, namespace, scriptEnvironmentVariables, environmentSetupScriptUrl, environmentTeardownScriptUrl,
            environmentConfigUrl, environmentDependencies, namespaceLazyCreateEnabled, namespaceCleanupEnabled,
            namespaceCleanupTimeout, namespaceCleanupConfirmationEnabled, namespaceDestroyEnabled,
            namespaceDestroyConfirmationEnabled, namespaceDestroyTimeout, waitEnabled, waitTimeout, waitPollInterval,
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
        this.token = token;
        this.routerSniPort = routerSniPort;
        this.templateLabels = templateLabels;
        this.templateParameters = templateParameters;
        this.templateURL = templateURL;
        this.templateProcess = templateProcess;
        this.username = username;
        this.password = password;
        this.apiVersion = apiVersion;
        this.trustCerts = trustCerts;
        this.startupTimeout = startupTimeout;
        this.httpClientTimeout = httpClientTimeout;
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
                    asURL(Strings.splitAndTrimAsList(getStringProperty(ENVIRONMENT_DEPENDENCIES, map, ""), "\\s*,\\s*")))
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

                .withWaitEnabled(getBooleanProperty(WAIT_ENABLED, map, true))
                .withWaitTimeout(getLongProperty(WAIT_TIMEOUT, map, DEFAULT_WAIT_TIMEOUT))
                .withWaitPollInterval(getLongProperty(WAIT_POLL_INTERVAL, map, DEFAULT_WAIT_POLL_INTERVAL))
                .withWaitForServiceList(
                    Strings.splitAndTrimAsList(getStringProperty(WAIT_FOR_SERVICE_LIST, map, ""), "\\s*,\\s*"))
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
                .withOpenshiftRouterHttpPort(getIntProperty(OPENSHIFT_ROUTER_HTTP_PORT, "openshift.router.httpPort", map, 80))
                .withOpenshiftRouterHttpsPort(getIntProperty(OPENSHIFT_ROUTER_HTTPS_PORT, "openshift.router.httpsPort", map, 443))
                .withEnableImageStreamDetection(getBooleanProperty(ENABLE_IMAGE_STREAM_DETECTION, map, true))
                .withToken(getStringProperty(AUTH_TOKEN, "kubernetes.auth.token", map, null))
                .withRouterSniPort(getIntProperty(ROUTER_SNI_PORT, "openshift.router.sniPort", map, 443))
                .withTemplateURL(getStringProperty(TEMPLATE_URL, "openshift.template.url", map, null))
                .withTemplateLabels(getStringProperty(TEMPLATE_LABELS, "openshift.template.labels", map, null))
                .withTemplateParameters(getStringProperty(TEMPLATE_PARAMETERS, "openshift.template.parameters", map, null))
                .withTemplateProcess(getBooleanProperty(TEMPLATE_PROCESS, "openshift.template.process", map, true))
                .withUsername(getStringProperty(USERNAME, "openshift.username", map, "guest"))
                .withPassword(getStringProperty(PASSWORD, "openshift.password", map, "guest"))
                .withApiVersion(getStringProperty(API_VERSION, "kubernetes.api.version", map, "v1"))
                .withTrustCerts(getBooleanProperty(TRUST_CERTS, "kubernetes.trust.certs", map, true))
                .withStartupTimeout(getLongProperty(STARTUP_TIMEOUT, "arquillian.startup.timeout", map, 600L))
                .withHttpClientTimeout(getLongProperty(HTTP_CLIENT_TIMEOUT, "arquillian.http.client.timeout", map, 120L))
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

    public void setClient(OpenShiftClient client) {
        this.client = client;
    }

    public int getRouterSniPort() {
        return routerSniPort;
    }

    @Override
    public String getToken() {

        if ((token == null || token.isEmpty()) && (client != null)) {
            token = client.getClientExt().getConfiguration().getOauthToken();
        }

        return token;
    }

    public Properties getProperties() {
        Properties properties = new Properties();
        apply(properties);
        return properties;
    }

    protected void apply(Properties properties) {
        // namespace
        properties.put("kubernetes.namespace", this.getNamespace());
        properties.put("namespace", this.getNamespace());
        // api version
        properties.put("version", getApiVersion());
        properties.put("kubernetes.api.version", getApiVersion());
    }

    public void validate() {

        if (isNullOrEmpty(this.getMasterUrl().toString()))
            throw new IllegalArgumentException("NULL master URL");

        if ((isNullOrEmpty(username) || isNullOrEmpty(password)) && isNullOrEmpty(token)) {
            throw new IllegalArgumentException("Missing OpenShift authentification -- username/password or token!");
        }
    }

    public String getTemplateURL() {
        return templateURL;
    }

    protected String getTemplateLabels() {
        return templateLabels;
    }

    public Map<String, String> getTemplateLabelsAsMap() {
        return org.arquillian.cube.openshift.impl.utils.Strings.splitKeyValueList(templateLabels);
    }

    public String getTemplateParameters() {
        return templateParameters;
    }

    public Map<String, String> getTemplateParametersAsMap() {
        return org.arquillian.cube.openshift.impl.utils.Strings.splitKeyValueList(templateParameters);
    }

    public boolean isTemplateProcess() {
        return templateProcess;
    }

    public org.arquillian.cube.kubernetes.api.Configuration getCubeConfiguration() {
        return this;
    }

    public String getKubernetesMaster() {
        return this.getMasterUrl().toString();
    }

    public boolean hasOpenshiftBasicAuth() {
        return isNotNullOrEmpty(username) && isNotNullOrEmpty(password);
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isTrustCerts() {
        return trustCerts;
    }

    public long getStartupTimeout() {
        return startupTimeout;
    }

    public long getHttpClientTimeout() {
        return httpClientTimeout;
    }

    public OpenShiftClient getClient() {
        return client;
    }

    public String getApiVersion() {
        return apiVersion;
    }
}
