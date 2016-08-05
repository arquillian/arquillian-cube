package org.arquillian.cube.kubernetes.api;

import java.util.Map;

/**
 * Created by iocanel on 8/1/16.
 */
public interface LabelProvider {

    Map<String, String> getLabels();
}
