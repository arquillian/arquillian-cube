package com.github.dockerjava.assertions;

import com.github.dockerjava.CubeOutput;
import org.assertj.core.api.AbstractAssert;

import static org.assertj.core.api.Assertions.assertThat;

public class CubeOutputAssert extends AbstractAssert<CubeOutputAssert, CubeOutput> {

    public CubeOutputAssert(CubeOutput actual) {
        super(actual, CubeOutputAssert.class);
    }

    public CubeOutputAssert hasProcessRunning(String processName) {
        isNotNull();

        assertThat(this.actual.getError()).isEmpty();
        assertThat(this.actual.getOutput()).isNotNull();
        assertThat(this.actual.getOutput()).isNotEmpty();

        assertThat(this.actual.getOutput()).contains(processName);

        return this;
    }

    public CubeOutputAssert hasProcessesRunning(String... processes) {
        isNotNull();

        assertThat(this.actual.getError()).isEmpty();
        assertThat(this.actual.getOutput()).isNotNull();
        assertThat(this.actual.getOutput()).isNotEmpty();

        assertThat(this.actual.getOutput()).contains(processes);

        return this;
    }
}
