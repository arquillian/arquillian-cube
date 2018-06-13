package org.arquillian.cube.kubernetes.impl.resolver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

public class ResourceResolver {

    static final String URL_PREFIX = "http";
    static final String FILE_PREFIX = "file";
    static final String CLASSPATH_PREFIX = "classpath:";

    private static File createTemporalDefinition(String content) throws IOException {
        // In case it is not a http, file nor a classpath protocol,
        // we assume that this is plain text. We store it in a temporary
        // file and return the URL to it.
        File tmp = File.createTempFile("arquillian-cube", ".res");

        // Remove the temporary file after running the test
        tmp.deleteOnExit();

        // Write content to temporary file
        BufferedWriter writer = new BufferedWriter(new FileWriter(tmp));
        writer.write(content);
        writer.close();

        return tmp;
    }

    public static URL resolve(String content) {
        try {
            if (content.startsWith(URL_PREFIX) || content.startsWith(FILE_PREFIX)) {
                return new URL(content);
            } else if (content.startsWith(CLASSPATH_PREFIX)) {
                String classPathLocation = content.substring(content.indexOf(CLASSPATH_PREFIX)
                    + CLASSPATH_PREFIX.length());
                final URL resource = Thread.currentThread().getContextClassLoader().getResource(classPathLocation);

                if (resource == null) {
                    throw new IllegalArgumentException(String.format("%s location couldn't be found inside classpath.", classPathLocation));
                }

                return resource;
            } else {
                File tmp = ResourceResolver.createTemporalDefinition(content);
                return tmp.toURI().toURL();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
