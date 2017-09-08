package org.arquillian.cube.openshift.impl.client.metadata;

import io.fabric8.kubernetes.clnt.v2_6.dsl.ExecWatch;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.arquillian.cube.impl.util.IOUtil;
import org.arquillian.cube.openshift.impl.client.OpenShiftClient;
import org.arquillian.cube.spi.metadata.CanCopyFromContainer;

public class CopyFromContainer implements CanCopyFromContainer {

    private final String cubeId;
    private final OpenShiftClient client;
    
    public CopyFromContainer(String cubeId, OpenShiftClient client) {
        this.cubeId = cubeId;
        this.client = client;
    }

    @Override
    public void copyDirectory(String from, String to) {
        Path toPath = Paths.get(to);
        File toPathFile = toPath.toFile();

        if(toPathFile.exists() && toPathFile.isFile()) {
            throw new IllegalArgumentException(String.format("%s parameter should be a directory in copy operation but you set an already existing file not a directory. Check %s in your local directory because currently is a file.", "to", toPath.normalize().toString()));
        }

        try {
            Files.createDirectories(toPath);
            final String fileOrDir;
            if (from.endsWith("/")) {
                fileOrDir = ".";
            } else {
                Path fromPath = FileSystems.getDefault().getPath(from);
                fileOrDir = fromPath.getFileName().toString();
                from = fromPath.getParent().toString();
            }
            try (ExecWatch watch = client.getClient().inNamespace(client.getClient().getNamespace()).pods().withName(cubeId).exec("tar", "-C", from, "-c", fileOrDir)) {
                IOUtil.untar(watch.getOutput(), toPathFile);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void copyLog(boolean follow, boolean stdout, boolean stderr, boolean timestamps, int tail,
            OutputStream outputStream) {
        if (!follow) {
            String log = client.getClient().inNamespace(client.getClient().getNamespace()).pods().withName(cubeId).getLog();
            try {
                outputStream.write(log.getBytes());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        } else {
            throw new IllegalArgumentException("log following not supported for pods");
        }
    }

}
