package org.arquillian.cube.openshift.operational.methods;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.kubernetes.client.readiness.Readiness;
import io.restassured.RestAssured;
import org.arquillian.cube.openshift.impl.client.OpenShiftAssistant;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;

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
    public void should_apply_template_programmatically() throws IOException {
        openShiftAssistant
                .usingTemplate(getClass().getClassLoader().getResource("hello-template.yaml"))
                .parameter("RESPONSE", "Hello from Arquillian Template")
            .deploy();

        final Optional<URL> route = openShiftAssistant.getRoute();
        openShiftAssistant.awaitUrl(route.get());

        RestAssured.given()
            .when()
            .get(route.get())
            .then()
            .assertThat()
            .statusCode(200)
            .body(is("Hello from Arquillian Template\n"));
    }

    @Test
    public void should_list_all_projects_and_verify_list_contains_the_project_with_given_name() {
        List<Project> projects = openShiftAssistant.listProjects();

        assertThat(projects).extracting(Project::getMetadata)
            .extracting(ObjectMeta::getName).contains(getCurrentProjectName());
    }

    @Test
    public void should_find_and_return_the_project_with_given_name() {
        Optional<Project> project = openShiftAssistant.findProject(getCurrentProjectName());
        
        assertThat(project).isPresent();
    }

    @Test
    public void should_check_if_the_project_with_given_name_exists() {
        boolean projectExists = openShiftAssistant.projectExists(getCurrentProjectName());
        
        assertThat(projectExists).isTrue();
    }

    @Test
    public void should_scale_project_and_verify_number_of_pods_is_expected() throws IOException {
        final String applicationName = "hello-openshift-scale-deployment-config";
        openShiftAssistant.deployApplication(applicationName, "hello-scale-template.yaml");
        openShiftAssistant.awaitApplicationReadinessOrFail();
        // scale to 2 using the last deployed project by openShiftAssistant
        openShiftAssistant.scale(2);

        List<Pod> pods = podsByLabelName(applicationName);

        assertThat(pods.size()).isEqualTo(2);
        assertThat(pods).allMatch(Readiness::isPodReady);

        // scale to 3 specifying target application name
        openShiftAssistant.scale(applicationName, 3);

        pods = podsByLabelName(applicationName);

        assertThat(pods.size()).isEqualTo(3);
        assertThat(pods).allMatch(Readiness::isPodReady);
    }

    private String getCurrentProjectName() {
        return openShiftAssistant.getCurrentProjectName();
    }

    private List<Pod> podsByLabelName(final String podLabelName) {
        return openShiftAssistant.getClient()
                .pods()
                .inNamespace(openShiftAssistant.getCurrentProjectName())
                .withLabel("name", podLabelName)
                .list()
                .getItems();
    }

}
