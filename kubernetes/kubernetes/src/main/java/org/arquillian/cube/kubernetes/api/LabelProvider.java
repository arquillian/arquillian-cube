package org.arquillian.cube.kubernetes.api;

import java.util.Map;

public interface LabelProvider {

    Map<String, String> getLabels();
}
