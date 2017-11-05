package org.arquillian.cube.kubernetes.api;

import java.util.concurrent.atomic.AtomicInteger;

public interface Session {

    String getId();

    Logger getLogger();

    String getNamespace();

    AtomicInteger getPassed();

    AtomicInteger getFailed();

    AtomicInteger getSkipped();

    void addListener(SessionListener listener);

    void removeListener(SessionListener listener);

    String getCurrentClassName();
    void setCurrentClassName(String className);

    String getCurrentMethodName();
    void setCurrentMethodName(String methodName);
}
