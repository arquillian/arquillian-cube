package org.arquillian.cube.docker.drone.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;

/**
 * Volume utils.
 */
public class VolumeCreator {

    private VolumeCreator() {
        super();
    }

    /**
     * Creates a new temporary folder with password file for VNC server.
     * The folder is relative to the root of the project target/vnc folder.
     *
     * @param password
     *     value to generate password file.
     *
     * @return Path of generated file.
     */
    public static final Path createTemporaryVolume(String password) {
        final File tmpFile = new File("target/vnc/.tmp" + System.currentTimeMillis());

        if (!tmpFile.mkdirs()) {
            throw new IllegalArgumentException("Temporary Folder for storing recordings could not be created.");
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                FileUtils.deleteQuietly(tmpFile);
            }
        });

        final Path temp = tmpFile.toPath();
        Path passwordFile = temp.resolve("password");

        try {
            Files.write(passwordFile, password.getBytes());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        return temp;
    }
}
