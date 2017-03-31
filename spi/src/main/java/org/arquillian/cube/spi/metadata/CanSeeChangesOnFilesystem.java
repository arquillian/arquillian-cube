package org.arquillian.cube.spi.metadata;

import java.util.List;

import org.arquillian.cube.ChangeLog;

public interface CanSeeChangesOnFilesystem extends CubeMetadata {

    List<ChangeLog> changes();
}
