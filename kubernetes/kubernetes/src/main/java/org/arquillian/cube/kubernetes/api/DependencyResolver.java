package org.arquillian.cube.kubernetes.api;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * Created by iocanel on 8/1/16.
 */
public interface DependencyResolver {

    /**
     * Resolves dependencies to additional kubernetes resources.
     * @param session       The session to resolve.
     * @return              A list of url to dependencies (as url to kubernetes resources)
     * @throws IOException  if dependencies cannot be resolved.
     */
    List<URL> resolve(Session session) throws IOException;
}
