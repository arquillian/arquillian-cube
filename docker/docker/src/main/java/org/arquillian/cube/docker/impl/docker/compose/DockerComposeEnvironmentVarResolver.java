package org.arquillian.cube.docker.impl.docker.compose;

import org.arquillian.cube.impl.util.IOUtil;

import java.io.InputStream;
import java.util.Map;

public class DockerComposeEnvironmentVarResolver {


    private DockerComposeEnvironmentVarResolver() {
        super();
    }

    /**
     * Method that takes an inputstream, read it preserving the end lines, and subtitute using commons-lang-3 calls
     * the variables, first searching as system properties vars and then in environment var list.
     * In case of missing the property is replaced by white space.
     * @param stream
     * @return
     */
    public static String replaceParameters(final InputStream stream) {
        String content = IOUtil.asStringPreservingNewLines(stream);
        return resolvePlaceholders(content);
    }

    private static String resolvePlaceholders(String content) {
        content = resolveSystemProperties(content);
        final Map<String, String> env = System.getenv();
        return IOUtil.replacePlaceholdersWithWhiteSpace(content, env);
    }

    private static String resolveSystemProperties(String content) {
        return IOUtil.replacePlaceholdersWithWhiteSpace(content);
    }

}
