package org.arquillian.cube.containerobject;

import com.jcabi.ssh.SSH;
import com.jcabi.ssh.Shell;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

@Category({RequiresDockerMachine.class, RequiresDocker.class})
@RequiresDockerMachine(name = "dev")
@RunWith(ArquillianConditionalRunner.class)
public class JSchIT {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Cube(value = "sftp-container", portBinding = "2222->22/tcp")
    SshdContainer sshdContainer;

    @Test
    public void shouldCopyFileToSFtp() throws JSchException, SftpException, IOException {
        String privateKey = IOUtils.toString(
            JSchIT.class.getResourceAsStream("/org/arquillian/cube/containerobject/SshdContainer/test_rsa"), "UTF-8");
        System.out.println(privateKey);
        String hello = new Shell.Plain(
            new SSH(
                sshdContainer.getIp(), 2222,
                "test", privateKey
            )
        ).exec("echo 'Hello, world!'");
        System.out.println(hello);
    }

    private File createFile() throws IOException {
        final File testFile = folder.newFile("testFile");
        FileUtils.write(testFile, "Hello World");
        return testFile;
    }
}
