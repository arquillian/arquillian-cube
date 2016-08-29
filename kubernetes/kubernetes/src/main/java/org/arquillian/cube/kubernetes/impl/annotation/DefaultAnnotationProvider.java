package org.arquillian.cube.kubernetes.impl.annotation;

import org.arquillian.cube.kubernetes.api.AnnotationProvider;

import java.util.HashMap;
import java.util.Map;

public class DefaultAnnotationProvider implements AnnotationProvider {

    public static final String SESSION_ID = "arquillian.org/test-session-id";
    public static final String TEST_SESSION_STATUS = "arquillian.org/test-session-status";


    @Override
    public Map<String, String> create(String sessionId, String status) {
        Map<String, String> annotations = new HashMap<>();
        annotations.put(SESSION_ID, sessionId);
        annotations.put(TEST_SESSION_STATUS, status);
        return annotations;
    }
}
