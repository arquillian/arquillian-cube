package org.arquillian.cube;

/**
 *
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
public interface CubeController {

    void start(String cubeId);

    void stop(String cubeId);

    void create(String cubeId);

    void destroy(String cubeId);
}
