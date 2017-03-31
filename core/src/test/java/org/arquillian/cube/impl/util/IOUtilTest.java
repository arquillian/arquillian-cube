package org.arquillian.cube.impl.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class IOUtilTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldTarFile() throws IOException {
        final File file = temporaryFolder.newFile("content.txt");
        final FileOutputStream output = new FileOutputStream(file);
        IOUtils.copy(new ByteArrayInputStream("hello".getBytes()), output);
        output.flush();
        output.close();

        File outputFolder = temporaryFolder.newFolder();
        IOUtil.tar(file, new File(outputFolder, "x.tar"));

        IOUtil.untar(new FileInputStream(new File(outputFolder, "x.tar")), outputFolder);
        final String content = IOUtil.asString(new FileInputStream(new File(outputFolder, "content.txt")));
        assertThat(content, is("hello"));
    }

    @Test
    public void shouldMergeToEmptyMap() {
        Map<String, Object> original = new HashMap<>();
        
        Map<String, Object> element = new HashMap<>();
        element.put("a", "b");
        Map<String, Object> element2 = new HashMap<>();
        element2.put("c", "d");
        element.put("e", element2);
        
        IOUtil.deepMerge(original, element);
        assertThat(original.containsKey("a"), is(true));
        assertThat(original.containsKey("e"), is(true));
        assertThat((String) original.get("a"), is("b"));
        Map<String, Object> innerElement = (Map<String, Object>) original.get("e");
        assertThat(innerElement.containsKey("c"), is(true));
        assertThat((String)innerElement.get("c"), is("d"));
    }

    @Test
    public void shouldAddSimpleAndComplexElements() {
        Map<String, Object> original = new HashMap<>();
        original.put("z", "y");

        Map<String, Object> element = new HashMap<>();
        element.put("a", "b");
        Map<String, Object> element2 = new HashMap<>();
        element2.put("c", "d");
        element.put("e", element2);

        IOUtil.deepMerge(original, element);
        assertThat(original.containsKey("a"), is(true));
        assertThat(original.containsKey("e"), is(true));
        assertThat(original.containsKey("z"), is(true));
        assertThat((String) original.get("a"), is("b"));
        assertThat((String) original.get("z"), is("y"));
        Map<String, Object> innerElement = (Map<String, Object>) original.get("e");
        assertThat(innerElement.containsKey("c"), is(true));
        assertThat((String)innerElement.get("c"), is("d"));
    }

    @Test
    public void shouldOverrideSimpleAndComplexElements() {
        Map<String, Object> original = new HashMap<>();
        Map<String, Object> org = new HashMap<>();
        org.put("a", "1");
        Map<String, Object> org2 = new HashMap<>();
        org2.put("c", "2");
        org.put("e", org2);


        Map<String, Object> element = new HashMap<>();
        element.put("a", "b");
        Map<String, Object> element2 = new HashMap<>();
        element2.put("c", "d");
        element.put("e", element2);

        IOUtil.deepMerge(original, element);
        assertThat(original.containsKey("a"), is(true));
        assertThat(original.containsKey("e"), is(true));
        assertThat((String) original.get("a"), is("b"));
        Map<String, Object> innerElement = (Map<String, Object>) original.get("e");
        assertThat(innerElement.containsKey("c"), is(true));
        assertThat((String)innerElement.get("c"), is("d"));
    }

}
