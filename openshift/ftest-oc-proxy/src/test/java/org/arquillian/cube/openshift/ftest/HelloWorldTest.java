package org.arquillian.cube.openshift.ftest;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import org.arquillian.cube.kubernetes.impl.utils.CommandExecutor;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

// tag::client_cli_execution[]
@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
public class HelloWorldTest {

    private static CommandExecutor commandExecutor = new CommandExecutor();

    @Test
    public void should_be_able_get_namespace_using_kubectl() {
        // when
        final List<String> namespaces = commandExecutor.execCommand("kubectl get ns -o=name");

        // then
        assertThat(namespaces).contains("namespaces/default");
    }

    @Test
    public void should_be_able_deploy_resources_using_oc() {
        // given
        String commandToExecute = "oc create -f " + getResource("openshift.json");

        // when
        final List<String> resources = commandExecutor.execCommand(commandToExecute);

        // then
        assertThat(resources).contains("service \"hello-world\" created", "deployment \"hello-world\" created");
    }

    @AfterClass
    public static void deleteDeployment() {
        String commandToExecute = "oc delete -f " + getResource("openshift.json");
        final List<String> strings = commandExecutor.execCommand(commandToExecute);

        assertThat(strings).contains("service \"hello-world\" deleted", "deployment \"hello-world\" deleted");
    }

    private static String getResource(String resourceName) {
        final URL resource = Thread.currentThread().getContextClassLoader().getResource(resourceName);
        if (resource == null) {
            throw new IllegalStateException("Expected " + resourceName + " to be on the classpath");
        }

        try {
            return resource.toURI().getPath();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
// end::client_cli_execution[]
