package org.arquillian.cube.kubernetes.api;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import java.net.URL;
import java.util.List;
import java.util.Map;

public interface Configuration {

    //Deprecated Property Names: {
    String KUBERNETES_MASTER = "kubernetes.master";
    String KUBERNETES_NAMESPACE = "kubernetes.namespace";
    String KUBERNETES_DOMAIN = "kubernetes.domain";
    // }

    String MASTER_URL = "master.url";
    String NAMESPACE = "namespace";
    String DOMAIN = "domain";

    String DOCKER_REGISTY = "docker.registry";
    String DOCKER_REGISTRY_HOST = "DOCKER_REGISTRY_HOST";
    String DOCKER_REGISTRY_PORT = "DOCKER_REGISTRY_PORT";
    String DOCKER_REGISTRY_FORMAT = "%s:%s";

    String NAMESPACE_LAZY_CREATE_ENABLED = "namespace.lazy.enabled";
    String NAMESPACE_CLEANUP_TIMEOUT = "namespace.cleanup.timeout";
    String NAMESPACE_CLEANUP_CONFIRM_ENABLED = "namespace.cleanup.confirm.enabled";
    String NAMESPACE_CLEANUP_ENABLED = "namespace.cleanup.enabled";
    String NAMESPACE_DESTROY_ENABLED = "namespace.destroy.enabled";
    String NAMESPACE_DESTROY_CONFIRM_ENABLED = "namespace.destroy.confirm.enabled";
    String NAMESPACE_DESTROY_TIMEOUT = "namespace.destroy.timeout";

    String NAMESPACE_USE_CURRENT = "namespace.use.current";
    String NAMESPACE_TO_USE = "namespace.use.existing";
    String NAMESPACE_PREFIX = "namespace.prefix";

    String ENVIRONMENT_INIT_ENABLED = "env.init.enabled";

    String ENVIRONMENT_SCRIPT_ENV = "env.script.env";
    String ENVIRONMENT_SETUP_SCRIPT_URL = "env.setup.script.url";
    String ENVIRONMENT_TEARDOWN_SCRIPT_URL = "env.teardown.script.url";
    String ENVIRONMENT_CONFIG_URL = "env.config.url";
    String ENVIRONMENT_CONFIG_RESOURCE_NAME = "env.config.resource.name";
    String ENVIRONMENT_DEPENDENCIES = "env.dependencies";
    String WAIT_FOR_ENVIRONMENT_DEPENDENCIES = "wait.for.env.dependencies";

    String WAIT_ENABLED = "wait.enabled";
    String WAIT_TIMEOUT = "wait.timeout";
    String WAIT_POLL_INTERVAL = "wait.poll.interval";

    String WAIT_FOR_SERVICE_LIST = "wait.for.service.list";

    String ANSI_LOGGER_ENABLED = "ansi.logger.enabled";
    String LOGS_COPY = "logs.copy";
    String LOGS_PATH = "logs.path";

    String USERNAME = "cube.username";
    String PASSWORD = "cube.password";
    String AUTH_TOKEN = "cube.auth.token";
    String API_VERSION = "cube.api.version";
    String TRUST_CERTS = "cube.trust.certs";

    // fabric8 maven plugin properties
    String FMP_BUILD = "cube.fmp.build";
    String FMP_BUILD_DISABLE_FOR_MAVEN = "cube.fmp.build.disable.for.mvn";
    String FMP_POM_PATH = "cube.fmp.pom.path";
    String FMP_DEBUG_OUTPUT = "cube.fmp.debug.output";
    String FMP_LOGS = "cube.fmp.logs";
    String FMP_PROFILES = "cube.fmp.profiles";
    String FMP_SYSTEM_PROPERTIES = "cube.fmp.system.properties";
    String FMP_BUILD_OPTIONS = "cube.fmp.build.options";
    String FMP_LOCAL_MAVEN = "cube.fmp.local.maven";

    Long DEFAULT_WAIT_TIMEOUT = 8 * 60 * 1000L;
    Long DEFAULT_WAIT_POLL_INTERVAL = 5 * 1000L;

    String DEFAULT_CONFIG_FILE_NAME = "kubernetes.json";
    String DEFAULT_FMP_PATH = "pom.xml";
    Long DEFAULT_NAMESPACE_CLEANUP_TIMEOUT = 0L;
    Long DEFAULT_NAMESPACE_DESTROY_TIMEOUT = 0L;
    Boolean DEFAULT_NAMESPACE_LAZY_CREATE_ENABLED = true;

    Config FALLBACK_CLIENT_CONFIG = new ConfigBuilder().build();

    URL getMasterUrl();

    Map<String, String> getScriptEnvironmentVariables();

    URL getEnvironmentSetupScriptUrl();

    URL getEnvironmentTeardownScriptUrl();

    URL getEnvironmentConfigUrl();

    List<URL> getEnvironmentDependencies();

    List<String> getWaitForEnvironmentDependencies();

    String getSessionId();

    String getNamespace();

    String getUsername();

    String getPassword();

    String getApiVersion();

    String getToken();

    boolean isTrustCerts();

    boolean isNamespaceLazyCreateEnabled();

    boolean isNamespaceCleanupEnabled();

    long getNamespaceCleanupTimeout();

    boolean isNamespaceCleanupConfirmationEnabled();

    boolean isNamespaceDestroyEnabled();

    boolean isNamespaceDestroyConfirmationEnabled();

    boolean isNamespaceUseCurrentEnabled();

    long getNamespaceDestroyTimeout();

    boolean isWaitEnabled();

    long getWaitTimeout();

    long getWaitPollInterval();

    List<String> getWaitForServiceList();

    boolean isAnsiLoggerEnabled();

    boolean isEnvironmentInitEnabled();

    boolean isLogCopyEnabled();

    boolean isFmpBuildForMavenDisable();

    boolean isFmpDebugOutput();

    boolean isFmpLogsEnabled();

    boolean isFmpBuildEnabled();

    String getFmpPomPath();

    String getFmpBuildOptions();

    boolean isFmpLocalMaven();

    List<String> getFmpProfiles();

    List<String> getFmpSystemProperties();

    String getLogPath();

    String getKubernetesDomain();

    String getDockerRegistry();
}
