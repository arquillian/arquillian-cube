package org.arquillian.cube.kubernetes.api;

import java.net.URL;
import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;

public interface ResourceInstaller {

    List<HasMetadata> install(URL url);
}
