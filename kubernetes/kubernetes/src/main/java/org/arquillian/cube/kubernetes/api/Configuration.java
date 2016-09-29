package org.arquillian.cube.kubernetes.api;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import org.arquillian.cube.impl.util.SystemEnvironmentVariables;

import java.net.URL;
import java.util.List;

public interface Configuration {

    String KUBERNETES_MASTER = "kubernetes.master";
    String KUBERNETES_NAMESPACE = "kubernetes.namespace";
    String KUBERNETES_DOMAIN = "kubernetes.domain";
    String DOCKER_REGISTY = "docker.registry";
    String DOCKER_REGISTRY_HOST = "DOCKER_REGISTRY_HOST";
    String DOCKER_REGISTRY_PORT = "DOCKER_REGISTRY_PORT";
    String DOCKER_REGISTRY_FORMAT = "%s:%s";


    String NAMESPACE_LAZY_CREATE_ENABLED = "namespace.lazy.enabled";
    String NAMESPACE_CLEANUP_TIMEOUT = "namespace.cleanup.timeout";
    String NAMESPACE_CLEANUP_CONFIRM_ENABLED = "namespace.cleanup.confirm.enabled";
    String NAMESPACE_CLEANUP_ENABLED = "namespace.cleanup.enabled";
    String NAMESPACE_TO_USE = "namespace.use.existing";
    String NAMESPACE_PREFIX = "namespace.prefix";

    String ENVIRONMENT_INIT_ENABLED = "env.init.enabled";
    String ENVIRONMENT_CONFIG_URL = "env.config.url";
    String ENVIRONMENT_CONFIG_RESOURCE_NAME = "env.config.resource.name";
    String ENVIRONMENT_DEPENDENCIES = "env.dependencies";

    String WAIT_TIMEOUT = "wait.timeout";
    String WAIT_POLL_INTERVAL = "wait.poll.interval";

    String WAIT_FOR_SERVICE_LIST = "wait.for.service.list";
    String WAIT_FOR_SERVICE_CONNECTION_ENABLED = "wait.for.service.connection.enabled";
    String WAIT_FOR_SERVICE_CONNECTION_TIMEOUT = "wait.for.service.connection.timeout";

    String ANSI_LOGGER_ENABLED = "ansi.logger.enabled";

        // Non-config constants
    String JAVA_PROTOCOL_HANDLER = "java.protocol.handler.pkgs";
    String PROTOCOL_HANDLERS = "protocolHandlers";
    String DEFAULT_MAVEN_PROTOCOL_HANDLER = "org.ops4j.pax.url";


    String DEFAULT_CONFIG_FILE_NAME = "kubernetes.json";
    Long DEFAULT_NAMESPACE_CLEANUP_TIMEOUT = 0L;
    Boolean DEFAULT_NAMESPACE_CLEANUP_ENABLED = true;
    Boolean DEFAULT_NAMESPACE_LAZY_CREATE_ENABLED = true;

    Config FALLBACK_CLIENT_CONFIG = new ConfigBuilder().build();


    URL getMasterUrl();

    URL getEnvironmentConfigUrl();

    List<URL> getEnvironmentDependencies();

    String getSessionId();

    String getNamespace();

    boolean isNamespaceLazyCreateEnabled();

    boolean isNamespaceCleanupEnabled();

    long getNamespaceCleanupTimeout();

    boolean isNamespaceCleanupConfirmationEnabled();

    long getWaitTimeout();

    long getWaitPollInterval();

    boolean isWaitForServiceConnectionEnabled();

    List<String> getWaitForServiceList();

    long getWaitForServiceConnectionTimeout();

    boolean isAnsiLoggerEnabled();

    boolean isEnvironmentInitEnabled();

    String getKubernetesDomain();

    String getDockerRegistry();

}