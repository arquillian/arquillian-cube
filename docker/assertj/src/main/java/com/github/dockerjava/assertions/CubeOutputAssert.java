package com.github.dockerjava.assertions;

import com.github.dockerjava.CubeOutput;
import java.util.Arrays;
import java.util.List;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.SoftAssertions;

public class CubeOutputAssert extends AbstractAssert<CubeOutputAssert, CubeOutput> {

    public CubeOutputAssert(CubeOutput actual) {
        super(actual, CubeOutputAssert.class);
    }

    public CubeOutputAssert hasProcessRunning(String processName) {
        isNotNull();

        List<String> processes = getProcesses(this.actual.getOutput());
        SoftAssertions softAssertions = new SoftAssertions();

        softAssertions.assertThat(this.actual.getError()).isEmpty();
        softAssertions.assertThat(processes)
            .overridingErrorMessage("Expected container's running process to contain <%s> but was %n <%s>", processName,
                processes)
            .contains(processName);

        softAssertions.assertAll();

        return this;
    }

    public CubeOutputAssert hasProcessesRunning(String... processes) {
        isNotNull();

        List<String> actualProcesses = getProcesses(this.actual.getOutput());

        SoftAssertions softAssertions = new SoftAssertions();

        softAssertions.assertThat(this.actual.getError()).isEmpty();
        softAssertions.assertThat(actualProcesses)
            .overridingErrorMessage("Expected container's running processes to contain %n <%s> but was %n <%s>",
                Arrays.asList(processes), actualProcesses)
            .contains(processes);

        softAssertions.assertAll();

        return this;
    }

    private List<String> getProcesses(String processes) {
        return Arrays.asList(processes.split("\n"));
    }
}
