package org.arquillian.cube;

/**
 *
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
public interface CubeController {

    void create(String cubeId);
    void create(CubeID cubeId);

    void start(String cubeId);
    void start(CubeID cubeId);

    void stop(String cubeId);
    void stop(CubeID cubeId);

    void destroy(String cubeId);
    void destroy(CubeID cubeId);
}
