package org.arquillian.cube.openshift.operational.methods;

import io.fabric8.kubernetes.api.model.v3_1.ObjectMeta;
import io.fabric8.openshift.api.model.v3_1.Project;
import org.arquillian.cube.openshift.impl.client.OpenShiftAssistant;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Category(RequiresOpenshift.class)
@RequiresOpenshift
@RunWith(ArquillianConditionalRunner.class)
public class OpenShiftAssistantOperationalMethodsIT {

    @ArquillianResource
    private OpenShiftAssistant openShiftAssistant;

    @Test
    public void should_inject_openshift_assistant() {
        assertThat(openShiftAssistant).isNotNull();
    }

    @Test
    public void should_list_all_projects() {
        // given
        String currentProject = openShiftAssistant.getCurrentProject();

        // when
        List<Project> projects = openShiftAssistant.listProjects();

        // then
        assertThat(projects).extracting(Project::getMetadata)
            .hasSize(2)
            .extracting(ObjectMeta::getName).contains(currentProject);
    }

    @Test
    public void should_find_project() {
        // given
        String currentProject = openShiftAssistant.getCurrentProject();

        // then
        assertThat(openShiftAssistant.findProject(currentProject)).isPresent();
    }

    @Test
    public void should_check_if_project_exists() {
        // given
        String currentProject = openShiftAssistant.getCurrentProject();

        // then
        assertThat(openShiftAssistant.projectExists(currentProject)).isTrue();
    }
}
