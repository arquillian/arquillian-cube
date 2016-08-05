package org.arquillian.cube.kubernetes.api;

import java.util.Map;

/**
 * Created by iocanel on 8/1/16.
 */
public interface AnnotationProvider {
    String SESSION_ID = "arquillian.org/test-session-id";
    String TEST_SESSION_STATUS = "arquillian.org/test-session-status";
    String TEST_CASE_STATUS_FORMAT = "fabric8.io/test-status-%s";
    int MAX_ANNOTATION_KEY_LENGTH = 63;


    Map<String, String> create(String sessionId, String status);
}
