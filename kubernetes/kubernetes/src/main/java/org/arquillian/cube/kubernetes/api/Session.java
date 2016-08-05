package org.arquillian.cube.kubernetes.api;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by iocanel on 7/30/16.
 */
public interface Session {

    String getId();
    Logger getLogger();
    String getNamespace();

    AtomicInteger getPassed();
    AtomicInteger getFailed();
    AtomicInteger getSkiped();

}
