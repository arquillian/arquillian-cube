package org.arquillian.cube;

/**
 * Factory to instantiate container objects
 *
 * @author <a href="mailto:rivasdiaz@gmail.com">Ramon Rivas</a>
 */
public interface ContainerObjectFactory {

    /**
     * Creates an instance of the container object. It also creates and starts a cube defined by the container object
     * class.
     *
     * @param containerObjectClass
     *     type of the container object to instantiate
     * @param <T>
     *     type of the container object
     *
     * @return the newly created container object
     */
    <T> T createContainerObject(Class<T> containerObjectClass);

    /**
     * Creates an instance of the container object. It also creates and starts a cube defined by the container object
     * class. Some configuration can be overridden by passing an additional ContainerObjectConfiguration instance.
     *
     * @param containerObjectClass
     *     type of the container object to instantiate
     * @param configuration
     *     allows specifying some configuration parameters
     * @param <T>
     *     type of the container object
     *
     * @return the newly created container object
     */
    <T> T createContainerObject(Class<T> containerObjectClass, ContainerObjectConfiguration configuration);

    /**
     * Creates an instance of the container object. It also creates and starts a cube defined by the container object
     * class. Some configuration can be overridden by passing an additional ContainerObjectConfiguration instance.
     *
     * @param containerObjectClass
     *     type of the container object to instantiate
     * @param configuration
     *     if not null, allows specifying some configuration parameters
     * @param containerObjectContainer
     *     marks this object as the container of the created container object
     * @param <T>
     *     type of the container object
     *
     * @return the newly created container object
     */
    <T> T createContainerObject(Class<T> containerObjectClass, ContainerObjectConfiguration configuration,
        Object containerObjectContainer);
}
