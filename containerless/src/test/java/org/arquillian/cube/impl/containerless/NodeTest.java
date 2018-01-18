package org.arquillian.cube.impl.containerless;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@Category({RequiresDocker.class, RequiresDockerMachine.class})
@RequiresDockerMachine(name = "dev")
@RunWith(ArquillianConditionalRunner.class)
public class NodeTest {

    @Deployment(testable = false)
    public static GenericArchive createDeployment() {
        return ShrinkWrap.create(GenericArchive.class, "app.tar")
            .add(new FileAsset(new File("src/test/js/index.js")), "index.js")
            .add(new FileAsset(new File("src/test/js/package.json")), "package.json");
    }

    @Test
    public void shouldReturnMessageFromNodeJs(@ArquillianResource URL base) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
            base.openStream()));) {
            String userInput = in.readLine();
            assertThat(userInput, is("Hello from inside a container!"));
        } catch (UnknownHostException e) {
            fail("Don't know about host ");
        } catch (IOException e) {
            fail("Couldn't get I/O for the connection to ");
        }
    }
}
