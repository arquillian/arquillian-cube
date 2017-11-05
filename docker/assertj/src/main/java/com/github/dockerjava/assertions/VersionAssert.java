package com.github.dockerjava.assertions;

import com.github.dockerjava.api.model.Version;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.util.Objects;

/**
 * @author Eddú Meléndez
 */
public class VersionAssert extends AbstractAssert<VersionAssert, Version> {

    public VersionAssert(Version actual) {
        super(actual, VersionAssert.class);
    }

    public VersionAssert isExperimental() {
        isNotNull();

        isExperimental(true);

        return this;
    }

    public VersionAssert isExperimental(boolean experimental) {
        isNotNull();

        if (!Objects.areEqual(this.actual.getExperimental(), experimental)) {
            failWithMessage("Expected docker's experimental to be %s but was %s", experimental,
                this.actual.getExperimental());
        }

        return this;
    }

    public VersionAssert hasApiVersion(String apiVersion) {
        isNotNull();

        if (!Objects.areEqual(this.actual.getApiVersion(), apiVersion)) {
            failWithMessage("Expected docker's apiVersion to be %s but was %s", apiVersion, this.actual.getApiVersion());
        }

        return this;
    }

    public VersionAssert hasVersion(String version) {
        isNotNull();

        if (!Objects.areEqual(this.actual.getVersion(), version)) {
            failWithMessage("Expected docker's version to be %s but was %s", version, this.actual.getVersion());
        }

        return this;
    }
}
