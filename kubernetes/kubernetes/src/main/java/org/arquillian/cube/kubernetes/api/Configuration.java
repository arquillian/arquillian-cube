package org.arquillian.cube.kubernetes.api;

import io.fabric8.kubernetes.clnt.v2_6.Config;
import io.fabric8.kubernetes.clnt.v2_6.ConfigBuilder;
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

    String WAIT_TIMEOUT = "wait.timeout";
    String WAIT_POLL_INTERVAL = "wait.poll.interval";

    String WAIT_FOR_SERVICE_LIST = "wait.for.service.list";

    String ANSI_LOGGER_ENABLED = "ansi.logger.enabled";
    String LOGS_COPY = "logs.copy";
    String LOGS_PATH = "logs.path";

    Long DEFAULT_WAIT_TIMEOUT = 5 * 60 * 1000L;
    Long DEFAULT_WAIT_POLL_INTERVAL = 5 * 1000L;

    String DEFAULT_CONFIG_FILE_NAME = "kubernetes.json";
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

    String getSessionId();

    String getNamespace();

    boolean isNamespaceLazyCreateEnabled();

    boolean isNamespaceCleanupEnabled();

    long getNamespaceCleanupTimeout();

    boolean isNamespaceCleanupConfirmationEnabled();

    boolean isNamespaceDestroyEnabled();

    boolean isNamespaceDestroyConfirmationEnabled();

    long getNamespaceDestroyTimeout();

    long getWaitTimeout();

    long getWaitPollInterval();

    List<String> getWaitForServiceList();

    boolean isAnsiLoggerEnabled();

    boolean isEnvironmentInitEnabled();

    boolean isLogCopyEnabled();

    String getLogPath();

    String getKubernetesDomain();

    String getDockerRegistry();
}
