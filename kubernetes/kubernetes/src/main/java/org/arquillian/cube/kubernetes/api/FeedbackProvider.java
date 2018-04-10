package org.arquillian.cube.kubernetes.api;

import io.fabric8.kubernetes.api.model.v3_1.HasMetadata;

public interface FeedbackProvider extends WithToImmutable<FeedbackProvider> {

    <T extends HasMetadata> void onResourceNotReady(T resource);
}
