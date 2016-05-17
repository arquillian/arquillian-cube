package org.arquillian.cube.docker.impl.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.arquillian.cube.docker.impl.client.CubeDockerConfiguration;
import org.junit.Assert;
import org.junit.Test;

public class AutoStartOrderUtilTestCase {

    private static final String SCENARIO_NO_KNOWN_LINK =
            "A:\n" +
            "  links:\n" +
            "    - B:B\n";

    private static final String SCENARIO_SINGLE_LINK =
            "A:\n" +
            "  links:\n" +
            "    - B:B\n" +
            "B:\n" +
            "  image: a\n";

    private static final String SCENARIO_MULTIPLE_ROOTS =
            "A:\n" +
            "  image: a\n" +
            "B:\n" +
            "  image: a\n";

    private static final String SCENARIO_MULTIPLE_ROOTS_AND_SINGLE_LINK =
            "A:\n" +
            "  links:\n" +
            "    - B:B\n" +
            "B:\n" +
            "  image: a\n" +
            "C:\n" +
            "  image: a\n";

    private static final String SCENARIO_SINGLE_ROOT_AND_MULTIPLE_LINKS =
            "A:\n" +
            "  links:\n" +
            "    - B:B\n" +
            "B:\n" +
            "  image: a\n" +
            "C:\n" +
            "  links:\n" +
            "    - B:B\n";

    private static final String SCENARIO_SINGLE_ROOT_AND_MULTIPLE_DEPENDS =
            "A:\n" +
            "  dependsOn:\n" +
            "    - B\n" +
            "B:\n" +
            "  image: a\n" +
            "C:\n" +
            "  dependsOn:\n" +
            "    - B\n";

    private static final String SCENARIO_SINGLE_ROOT_AND_MULTIPLE_DEPENDS_AND_LINK =
            "A:\n" +
            "  dependsOn:\n" +
            "    - B\n" +
            "B:\n" +
            "  image: a\n" +
            "C:\n" +
            "  links:\n" +
            "    - A:A\n" +
            "  dependsOn:\n" +
            "    - B\n";

    private static final String SCENARIO_MULTI_ROOT_AND_MULTIPLE_LINKS =
            "A:\n" +
            "  links:\n" +
            "    - B:B\n" +
            "    - D:D\n" +
            "B:\n" +
            "  image: a\n" +
            "C:\n" +
            "  links:\n" +
            "    - B:B\n" +
            "D:\n" +
            "  links:\n" +
            "    - E:E\n" +
            "E:\n" +
            "  image: a\n" +
            "F:\n" +
            "  links:\n" +
            "    - E:E\n" +
            "    - C:C\n"  ;

    private static final String SCENARIO_RECURSIVE_LINKS =
            "A:\n" +
            "  links:\n" +
            "    - B:B\n" +
            "B:\n" +
            "  links:\n" +
            "    - A:A\n";

    @Test
    public void shouldSortNoKnownLinks() throws Exception {
        List<String[]> sorted = AutoStartOrderUtil.getAutoStartOrder(
                create(SCENARIO_NO_KNOWN_LINK, "A"));

        assertExecutionSteps(sorted, new String[]{"A"});
    }

    @Test
    public void shouldSortSingleLink() throws Exception {
        List<String[]> sorted = AutoStartOrderUtil.getAutoStartOrder(
                create(SCENARIO_SINGLE_LINK, "A"));

        assertExecutionSteps(sorted, new String[]{"B"}, new String[]{"A"});
    }

    @Test
    public void shouldSortSingleNonLinkedRoots() throws Exception {
        List<String[]> sorted = AutoStartOrderUtil.getAutoStartOrder(
                create(SCENARIO_MULTIPLE_ROOTS, "A"));

        assertExecutionSteps(sorted, new String[]{"A"});
    }

    @Test
    public void shouldSortMultipleNonLinkedRoots() throws Exception {
        List<String[]> sorted = AutoStartOrderUtil.getAutoStartOrder(
                create(SCENARIO_MULTIPLE_ROOTS, "A", "B"));

        assertExecutionSteps(sorted, new String[]{"A", "B"});
    }

    @Test
    public void shouldSortMultipleRootWithSingleLinks() throws Exception {
        List<String[]> sorted = AutoStartOrderUtil.getAutoStartOrder(
                create(SCENARIO_MULTIPLE_ROOTS_AND_SINGLE_LINK, "C", "A"));

        assertExecutionSteps(sorted, new String[]{"B", "C"}, new String[]{"A"});
    }

    @Test
    public void shouldSortSingleRootWithMultipleLinks() throws Exception {
        List<String[]> sorted = AutoStartOrderUtil.getAutoStartOrder(
                create(SCENARIO_SINGLE_ROOT_AND_MULTIPLE_LINKS, "C", "A"));

        assertExecutionSteps(sorted, new String[]{"B"}, new String[]{"A", "C"});
    }

    @Test
    public void shouldSortSingleRootWithMultipleDependsOn() throws Exception {
        List<String[]> sorted = AutoStartOrderUtil.getAutoStartOrder(
                create(SCENARIO_SINGLE_ROOT_AND_MULTIPLE_DEPENDS, "C", "A"));

        assertExecutionSteps(sorted, new String[]{"B"}, new String[]{"A", "C"});
    }

    @Test
    public void shouldSortSingleRootWithMultipleDependsOnMustHavePriorityOverLinks() throws Exception {
        List<String[]> sorted = AutoStartOrderUtil.getAutoStartOrder(
                create(SCENARIO_SINGLE_ROOT_AND_MULTIPLE_DEPENDS_AND_LINK, "C", "A"));

        assertExecutionSteps(sorted, new String[]{"B"}, new String[]{"A", "C"});
    }

    @Test
    public void shouldSortMultiRootWithMultipleLinks() throws Exception {
        List<String[]> sorted = AutoStartOrderUtil.getAutoStartOrder(
                create(SCENARIO_MULTI_ROOT_AND_MULTIPLE_LINKS, "A", "F"));

        assertExecutionSteps(sorted, new String[]{"B", "E"}, new String[]{"D", "C"}, new String[]{"A", "F"});
    }

    @Test
    public void shouldSortInReverseMultiRootWithMultipleLinks() throws Exception {
        List<String[]> sorted = AutoStartOrderUtil.getAutoStopOrder(
                create(SCENARIO_MULTI_ROOT_AND_MULTIPLE_LINKS, "A", "F"));

        assertExecutionSteps(sorted, new String[]{"A", "F"}, new String[]{"D", "C"}, new String[]{"B", "E"});
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailOnRecursiveLinks() throws Exception {
        AutoStartOrderUtil.getAutoStartOrder(
                create(SCENARIO_RECURSIVE_LINKS, "A", "B"));
    }

    private void assertExecutionSteps(List<String[]> actuals, String[]... expecteds) {
        Assert.assertEquals("Number of steps to should match", expecteds.length, actuals.size());

        for(int i = 0; i < actuals.size(); i++) {
            List<String> actual = Arrays.asList(actuals.get(i));
            String[] expected = expecteds[i];
            Assert.assertEquals("Number of cubes in step[" + i +"] should match", expected.length, actual.size());

            for(String expectedId : expected) {
                Assert.assertTrue("Cube[" + expectedId + "] should have been in step[" + i + "] Found[" + join(actual) + "]", actual.contains(expectedId));
            }
        }
    }

    private CubeDockerConfiguration create(String setup, String... autoStart) {
        Map<String, String> config = new HashMap<>();
        if(autoStart != null && autoStart.length > 0) {
            config.put("autoStartContainers", join(autoStart));
        }
        config.put("dockerContainers", setup);
        return CubeDockerConfiguration.fromMap(config);
    }

    private String join(Collection<String> strings) {
        return join(strings.toArray(new String[]{}));
    }

    private String join(String... auto) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < auto.length; i++) {
            sb.append(auto[i]);
            if(i < auto.length -1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }
}