package org.arquillian.cube.spi.metadata;

public interface CanCopyToContainer extends CubeMetadata {
    void copyDirectory(String from, String to);
    void copyDirectory(String from);
}
