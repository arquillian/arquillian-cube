package com.github.dockerjava.assertions;

import com.github.dockerjava.api.model.Image;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.assertj.core.api.ListAssert;

/**
 * @author Eddú Meléndez
 */
public class ImagesAssert extends ListAssert<Image> {

    public ImagesAssert(List<? extends Image> actual) {
        super(actual);
    }

    public ImagesAssert containsImages(String... imageIds) {
        List<String> imageList = new ArrayList<String>();
        for (Image image : this.actual) {
            imageList.add(image.getId());
        }

        org.assertj.core.api.Assertions.assertThat(this.actual)
            .extracting("id")
            .overridingErrorMessage("%nExpecting:%n <%s>%nto contain:%n <%s>", imageList, Arrays.asList(imageIds))
            .contains(imageIds);

        return this;
    }
}
