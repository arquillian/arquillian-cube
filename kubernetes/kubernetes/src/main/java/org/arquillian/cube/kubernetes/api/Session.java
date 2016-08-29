package org.arquillian.cube.kubernetes.api;

import java.util.concurrent.atomic.AtomicInteger;

public interface Session {

    String getId();
    Logger getLogger();
    String getNamespace();

    AtomicInteger getPassed();
    AtomicInteger getFailed();
    AtomicInteger getSkiped();

}
