package org.arquillian.cube.containerobject;

import java.io.File;
import java.nio.file.Files;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;

import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@Category({ RequiresDocker.class})

@RunWith(ArquillianConditionalRunner.class)
public class FtpClientIT {

    public static final String REMOTE_FILENAME = "a.txt";
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    @Cube
    FtpContainer ftpContainer;

    @Test
    public void should_upload_file_to_ftp_server() throws Exception {

        // Given
        final File file = folder.newFile(REMOTE_FILENAME);
        Files.write(file.toPath(), "Hello World".getBytes());

        // When
        FtpClient ftpClient = new FtpClient(ftpContainer.getIp(),
            ftpContainer.getBindPort(),
            ftpContainer.getUsername(), ftpContainer.getPassword());
        try {
            ftpClient.uploadFile(file, REMOTE_FILENAME);
        } finally {
            ftpClient.disconnect();
        }

        // Then
        final boolean filePresentInContainer = ftpContainer.isFilePresentInContainer(REMOTE_FILENAME);
        assertThat(filePresentInContainer, is(true));
    }
}
