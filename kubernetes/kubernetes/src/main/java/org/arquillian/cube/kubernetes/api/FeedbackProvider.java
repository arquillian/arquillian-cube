package org.arquillian.cube.kubernetes.api;

import io.fabric8.kubernetes.api.model.v2_5.HasMetadata;

public interface FeedbackProvider extends WithToImmutable<FeedbackProvider> {

    <T extends HasMetadata> void onResourceNotReady(T resource);
}
