package org.arquillian.cube.kubernetes.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.arquillian.cube.kubernetes.api.Logger;
import org.arquillian.cube.kubernetes.api.Session;
import org.arquillian.cube.kubernetes.api.SessionListener;

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
    private final List<SessionListener> listeners = new ArrayList<>();

    private String currentClassName;
    private String currentMethodName;

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

        for (SessionListener listener : listeners) {
            try {
                listener.onClose();
            } catch (Throwable t) {
                logger.warn("Error calling session listener: [" + listener + "]");
            }
        }
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

    @Override
    public void addListener(SessionListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(SessionListener listener) {
        listeners.remove(listener);
    }

    @Override
    public String getCurrentClassName() {
        return currentClassName;
    }

    @Override
    public void setCurrentClassName(String className) {
        this.currentClassName = className;
    }

    @Override
    public String getCurrentMethodName() {
        return currentMethodName;
    }

    @Override
    public void setCurrentMethodName(String methodName) {
        this.currentMethodName = methodName;
    }
}
