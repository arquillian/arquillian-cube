package org.arquillian.cube.kubernetes.api;

import io.fabric8.kubernetes.api.model.v2_6.Namespace;
import java.util.Map;

public interface NamespaceService extends WithToImmutable<NamespaceService> {

    /**
     * Creates a {@link Namespace} with the specified name.
     *
     * @param namespace
     *     The name of the {@link Namespace}.
     *
     * @return The created {@link Namespace}.
     */
    Namespace create(String namespace);

    /**
     * Creates a {@link Namespace} with the specified name.
     *
     * @param namespace
     *     The name of the {@link Namespace}.
     * @param annotations
     *     A map containing the annotations.
     *
     * @return The created {@link Namespace}.
     */
    Namespace create(String namespace, Map<String, String> annotations);

    /**
     * Adds the specified annotations to the {@link Namespace}.
     *
     * @param namespace
     *     The {@link Namespace} to annotate.
     * @param annotations
     *     A map containing the annotations.
     *
     * @return The annotated {@link Namespace}.
     */
    Namespace annotate(String namespace, Map<String, String> annotations);

    /**
     * Deletes the specified {@link Namespace}.
     *
     * @param namespace
     *     The name of the {@link Namespace} to delete.
     *
     * @return True if it was succesfully delete, False otherwise.
     */
    Boolean delete(String namespace);

    /**
     * Checks if {@link Namespace} can exists.
     *
     * @param namespace
     *     The name of the {@link Namespace} to check.
     *
     * @return True if {@link Namespace} exists, False otherwise.
     */
    Boolean exists(String namespace);

    /**
     * Clears all resources from the specified {@link Namespace}/
     */
    @Deprecated // The method is redundant (since its called always before destroy).
    void clean(String namespace);

    /**
     * Destroy the {@link Namespace}.
     *
     * @param namespace
     *     The namespace to destroy.
     */
    void destroy(String namespace);
}


