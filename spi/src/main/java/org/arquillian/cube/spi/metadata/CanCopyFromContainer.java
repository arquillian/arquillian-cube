package org.arquillian.cube.spi.metadata;

import java.io.OutputStream;

public interface CanCopyFromContainer extends CubeMetadata {

    void copyDirectory(String from, String to);

    void copyLog(boolean follow, boolean stdout, boolean stderr, boolean timestamps, int tail, OutputStream outputStream);
}
