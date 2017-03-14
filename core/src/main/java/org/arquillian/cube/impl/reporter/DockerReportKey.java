package org.arquillian.cube.impl.reporter;

import org.arquillian.reporter.api.model.AbstractStringKey;
import org.arquillian.reporter.api.model.StringKey;

public class DockerReportKey extends AbstractStringKey {

    //section name
    public static final StringKey DOCKER_CONTAINER_CONFIGURATION = new DockerReportKey();

    //elements
    public static final StringKey ERROR_DURING_LIFECYCLE = new DockerReportKey();
    public static final StringKey STARTING_TIME = new DockerReportKey();
    public static final StringKey STOPPING_TIME = new DockerReportKey();
    public static final StringKey CONTAINER_NAME = new DockerReportKey();
    public static final StringKey IMAGE_NAME = new DockerReportKey();
    public static final StringKey BUILD_LOCATION = new DockerReportKey();
    public static final StringKey EXPOSED_PORTS = new DockerReportKey();
    public static final StringKey PORT_BINDING = new DockerReportKey();
    public static final StringKey LINKS = new DockerReportKey();
    public static final StringKey VOLUMES = new DockerReportKey();
    public static final StringKey BINDS = new DockerReportKey();
    public static final StringKey ENTRY_POINT = new DockerReportKey();
    public static final StringKey NETWORK_MODE = new DockerReportKey();

}
