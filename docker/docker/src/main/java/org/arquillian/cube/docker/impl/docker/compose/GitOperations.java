package org.arquillian.cube.docker.impl.docker.compose;

import java.io.File;
import java.io.IOException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import static java.nio.file.Files.createTempDirectory;

public class GitOperations {

    public File cloneRepo(String uri) {

        Git git = null;
        try {
            git = Git.cloneRepository()
                .setURI(uri)
                .setDirectory(createTempDirectory("cubeClone").toFile())
                .call();

            return git.getRepository().getDirectory();
        } catch (GitAPIException e) {
            throw new IllegalArgumentException(e);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        } finally {
            if (git != null) {
                git.close();
            }
        }
    }
}
