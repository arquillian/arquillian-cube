package org.arquillian.cube.kubernetes.impl;


import org.arquillian.cube.kubernetes.api.Logger;
import org.arquillian.cube.kubernetes.api.Session;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a testing session.
 * It is used for scoping pods, service and replication controllers created during the test.
 */
public class DefaultSession implements Session {
    private final String id;
    private final Logger logger;
    private final String namespace;
    private final AtomicInteger passed = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private final AtomicInteger skipped = new AtomicInteger();

    public DefaultSession(String id, String namespace, Logger logger) {
        this.id = id;
        this.logger = logger;
        this.namespace = namespace;
    }
    void init() {
        logger.status("Initializing Session:" + id);
    }

    void destroy() {
        logger.status("Destroying Session:" + id);
        System.out.flush();
    }

    public String getId() {
        return id;
    }

    public Logger getLogger() {
        return logger;
    }

    /**
     * Returns the namespace ID for this test case session
     */
    public String getNamespace() {
        return namespace;
    }

    public AtomicInteger getPassed() {
        return passed;
    }

    public AtomicInteger getFailed() {
        return failed;
    }

    public AtomicInteger getSkipped() {
        return skipped;
    }
}
