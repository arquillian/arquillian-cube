package org.arquillian.cube;

/**
 * Configuration options that can be overridden at the moment of creating a new instance of a container object.
 * All options in this class can also be set with annotations for container objects referenced as fields
 *
 * @author <a href="mailto:rivasdiaz@gmail.com">Ramon Rivas</a>
 */
public interface ContainerObjectConfiguration {

    String getContainerName();

    String[] getPortBindings();

    int[] getAwaitPorts();

    String[] getEnvironmentVariables();

    String[] getVolumes();

    String[] getLinks();
}
