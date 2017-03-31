package org.arquillian.cube.docker.impl.client.containerobject.dsl;

import com.github.dockerjava.api.model.AccessMode;

/**
 * Binding mode for volumes
 */
public enum BindMode {

    READ_ONLY(AccessMode.ro), READ_WRITE(AccessMode.rw);

    public final AccessMode accessMode;

    BindMode(AccessMode accessMode) {
        this.accessMode = accessMode;
    }
}
