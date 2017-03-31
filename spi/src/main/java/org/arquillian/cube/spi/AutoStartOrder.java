package org.arquillian.cube.spi;

import java.util.List;

/**
 * Interface that returns for given configuration object the order for starting and stopping cubes.
 *
 * @param <T>
 *     Definition of cubes
 */
public interface AutoStartOrder<T> {

    /**
     * Returns a list of Ids of Cubes to start. All the elements of the array are going to be started in parallel.
     *
     * @param config
     *     Configuration object.
     *
     * @return List of cubes to start. Each element of the list is an array of identifiers. All elements of the array are
     * started in parallel.
     */
    List<String[]> getAutoStartOrder(T config);

    /**
     * Returns a list of Ids of Cubes to stop. All the elements of the array are going to be stopped in parallel.
     *
     * @param config
     *     Configuration object.
     *
     * @return List of cubes to stop. Each element of the list is an array of identifiers. All elements of the array are
     * stopped in parallel.
     */
    List<String[]> getAutoStopOrder(T config);
}
