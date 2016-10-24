package org.arquillian.cube.kubernetes.api;


public interface SessionListener {

    /**
     * Called when session is closed.
     */
    void onClose();
}
