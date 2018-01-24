package org.arquillian.cube.openshift.operational.methods;

import io.fabric8.kubernetes.api.model.v3_1.ObjectMeta;
import io.fabric8.openshift.api.model.v3_1.Project;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
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

        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().get().url(route.get()).build();
        Response response = okHttpClient.newCall(request).execute();

        assertThat(response).isNotNull();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body().string()).isEqualTo("Hello from Arquillian Template\n");
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

    private String getCurrentProjectName() {
        return openShiftAssistant.getCurrentProjectName();
    }
}
