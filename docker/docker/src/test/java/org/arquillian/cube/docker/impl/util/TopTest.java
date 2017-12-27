package org.arquillian.cube.docker.impl.util;

import java.io.IOException;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@Category({RequiresDocker.class, RequiresDockerMachine.class})
public class TopTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldStopSpinningIfRunningInsideDocker() throws IOException {
        temporaryFolder.newFile(Top.DOCKER_SOCK);
        temporaryFolder.newFile(Top.DOCKERENV);
        temporaryFolder.newFile(Top.DOCKERINIT);
        Top top = new Top(temporaryFolder.getRoot().getAbsolutePath(), temporaryFolder.getRoot().getAbsolutePath());
        assertThat(top.isSpinning(), is(true));
    }

    @Test
    public void shouldNotStopSpinningIfRunningInsideDocker() throws IOException {
        temporaryFolder.newFile(Top.DOCKERENV);
        temporaryFolder.newFile(Top.DOCKERINIT);
        Top top = new Top(temporaryFolder.getRoot().getAbsolutePath(), temporaryFolder.getRoot().getAbsolutePath());
        assertThat(top.isSpinning(), is(false));
    }
}
