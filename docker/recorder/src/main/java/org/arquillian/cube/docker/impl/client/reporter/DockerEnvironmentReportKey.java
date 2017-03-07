package org.arquillian.cube.docker.impl.client.reporter;

import org.arquillian.cube.impl.reporter.DockerReportKey;
import org.arquillian.reporter.api.model.AbstractStringKey;
import org.arquillian.reporter.api.model.StringKey;

public class DockerEnvironmentReportKey extends AbstractStringKey {

    //section nme
    public static final StringKey DOCKER_ENVIRONMENT_SECTION_NAME = new DockerEnvironmentReportKey();
    public static final StringKey DOCKER_INFO_SECTION_NAME = new DockerEnvironmentReportKey();

    //elements
    public static final StringKey DOCKER_VERSION = new DockerReportKey();
    public static final StringKey DOCKER_OS = new DockerReportKey();
    public static final StringKey DOCKER_KERNEL = new DockerReportKey();
    public static final StringKey DOCKER_API_VERSION = new DockerReportKey();
    public static final StringKey DOCKER_ARCH = new DockerReportKey();

    public static final StringKey DOCKER_COMPOSITION_SCHEMA = new DockerReportKey();
    public static final StringKey NETWORK_TOPOLOGY_SCHEMA = new DockerReportKey();

    public static final StringKey MEMORY_STATISTICS = new DockerReportKey();
    public static final StringKey USAGE = new DockerReportKey();
    public static final StringKey MAX_USAGE = new DockerReportKey();
    public static final StringKey LIMIT = new DockerReportKey();

    public static final StringKey BEFORE = new DockerReportKey();
    public static final StringKey AFTER = new DockerReportKey();


    public static final StringKey LOG_PATH = new DockerReportKey();
    public static final StringKey VIDEO_PATH = new DockerReportKey();

}
