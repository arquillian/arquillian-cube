package org.arquillian.cube.openshift.ftest;

import java.util.List;
import org.arquillian.cube.kubernetes.impl.utils.CommandExecutor;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
public class HelloWorldTest {

    private CommandExecutor commandExecutor;

    @Before
    public void initCommandExecutor() {
        commandExecutor = new CommandExecutor();
    }

    @Test
    public void should_be_able_deploy_resources_using_oc() {
        // when
        final List<String> resources = commandExecutor.execCommand("oc create -f https://goo.gl/qaiGRJ");

        // then
        assertThat(resources).contains("service \"hello-world\" created", "deployment \"hello-world\" created");
    }

    @Test
    public void should_be_able_get_namespace_using_kubectl() {
        // when
        final List<String> namespaces = commandExecutor.execCommand("kubectl get ns -o=name");

        // then
        assertThat(namespaces).contains("namespaces/default");
    }

    @After
    public void deleteDeployment() {
        final List<String> strings = commandExecutor.execCommand("oc delete -f https://goo.gl/qaiGRJ");

        assertThat(strings).contains("service \"hello-world\" deleted", "deployment \"hello-world\" deleted");
    }
}
