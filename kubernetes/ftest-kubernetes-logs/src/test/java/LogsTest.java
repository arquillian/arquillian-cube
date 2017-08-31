import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.arquillian.cube.kubernetes.impl.requirement.RequiresKubernetes;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ArquillianConditionalRunner.class)
@RequiresKubernetes
public class LogsTest {

    @Test
    public void testSingleContainer() throws IOException {
        String filename = System.getProperty("user.dir") + "/target/surefire-reports/NOCLASS-test-single-container.log";
        String contents = FileUtils.readFileToString(new File(filename));
        assertTrue(contents.contains("only-one"));
    }

    @Test
    public void testMultipleContainers() throws IOException {
        String filename = System.getProperty("user.dir")
                + "/target/surefire-reports/NOCLASS-test-multiple-containers-first-container.log";
        String contents = FileUtils.readFileToString(new File(filename));
        assertTrue(contents.contains("first"));

        filename = System.getProperty("user.dir")
                + "/target/surefire-reports/NOCLASS-test-multiple-containers-second-container.log";
        contents = FileUtils.readFileToString(new File(filename));
        assertTrue(contents.contains("second"));
    }

}
