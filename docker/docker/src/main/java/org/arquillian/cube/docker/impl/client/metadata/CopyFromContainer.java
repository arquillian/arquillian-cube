package org.arquillian.cube.docker.impl.client.metadata;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.arquillian.cube.docker.impl.docker.DockerClientExecutor;
import org.arquillian.cube.impl.util.IOUtil;
import org.arquillian.cube.spi.metadata.CanCopyFromContainer;

public class CopyFromContainer implements CanCopyFromContainer {

    private String cubeId;
    private DockerClientExecutor executor;

    public CopyFromContainer(String cubeId, DockerClientExecutor executor) {
        this.cubeId = cubeId;
        this.executor = executor;
    }

    @Override
    public void copyDirectory(String from, String to) {
        InputStream response = executor.getFileOrDirectoryFromContainerAsTar(cubeId, from);

        Path toPath = Paths.get(to);
        File toPathFile = toPath.toFile();

        if(toPathFile.exists() && toPathFile.isFile()) {
            throw new IllegalArgumentException(String.format("%s parameter should be a directory in copy operation but you set an already existing file not a directory. Check %s in your local directory because currently is a file.", "to", toPath.normalize().toString()));
        }

        try {
            Files.createDirectories(toPath);
            IOUtil.untar(response, toPathFile);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void copyLog(boolean follow, boolean stdout, boolean stderr, boolean timestamps, int tail, OutputStream outputStream) {
        try {
            executor.copyLog(cubeId, follow, stdout, stderr, timestamps, tail, outputStream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
