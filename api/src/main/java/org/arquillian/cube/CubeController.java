package org.arquillian.cube;

import java.io.OutputStream;
import java.util.List;


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

    void copyFileDirectoryFromContainer(String cubeId, String from, String to);
    void copyFileDirectoryFromContainer(CubeID cubeId, String from, String to);

    List<ChangeLog> changesOnFilesystem(String cubeId);
    List<ChangeLog> changesOnFilesystem(CubeID cubeId);

    TopContainer top(String cubeId);
    TopContainer top(CubeID cubeId);

    //This method will be refactored when we have a model for configuration file.
    void copyLog(String cubeId, boolean follow, boolean stdout, boolean stderr, boolean timestamps, int tail, OutputStream outputStream);
    void copyLog(CubeID cubeId, boolean follow, boolean stdout, boolean stderr, boolean timestamps, int tail, OutputStream outputStream);
}
