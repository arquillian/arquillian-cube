package com.github.dockerjava.assertions;

import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.ContainerConfig;
import java.util.Arrays;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.data.MapEntry;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Eddú Meléndez
 */
public class ImageAssert extends AbstractAssert<ImageAssert, InspectImageResponse> {

    public ImageAssert(InspectImageResponse actual) {
        super(actual, ImageAssert.class);
    }

    public ImageAssert hasLabels(String... labels) {
        isNotNull();

        assertThat(getImageConfig().getLabels())
            .overridingErrorMessage("%nExpecting:%n <%s>%nto contain:%n <%s>", getImageConfig().getLabels().keySet(),
                Arrays.asList(labels))
            .containsKeys(labels);

        return this;
    }

    public ImageAssert hasLabel(String label, String value) {
        isNotNull();

        assertThat(getImageConfig().getLabels())
            .overridingErrorMessage("%nExpecting:%n <%s>%nto contain:%n <%s>", getImageConfig().getLabels(),
                MapEntry.entry(label, value))
            .containsEntry(label, value);

        return this;
    }

    private ContainerConfig getImageConfig() {
        return this.actual.getConfig();
    }
}
