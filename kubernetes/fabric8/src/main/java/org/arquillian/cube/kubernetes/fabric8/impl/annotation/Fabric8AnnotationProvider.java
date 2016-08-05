package org.arquillian.cube.kubernetes.fabric8.impl.annotation;

import org.apache.commons.codec.binary.StringUtils;
import org.arquillian.cube.impl.util.IOUtil;
import org.arquillian.cube.impl.util.Strings;
import org.arquillian.cube.impl.util.SystemEnvironmentVariables;
import org.arquillian.cube.kubernetes.api.AnnotationProvider;
import org.arquillian.cube.kubernetes.api.Logger;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by iocanel on 8/5/16.
 */
public class Fabric8AnnotationProvider implements AnnotationProvider {

    public static final String GIT_COMMIT = "fabric8.io/git-commit";
    public static final String GIT_URL = "fabric8.io/git-url";
    public static final String GIT_BRANCH = "fabric8.io/git-branch";
    public static final String PROJECT_PREFIX = "fabric8.io/project-";

    @Inject
    @ApplicationScoped
    Instance<Logger> logger;

    @Override
    public Map<String, String> create(String sessionId, String status) {
        Map<String, String> annotations = new HashMap<>();
        annotations.put(SESSION_ID, sessionId);
        annotations.put(TEST_SESSION_STATUS, status);

        File baseDir = getProjectBaseDir();
        String gitUrl = getGitUrl(baseDir);
        if (Strings.isNotNullOrEmpty(gitUrl)) {
            annotations.put(GIT_URL, gitUrl);
        }
        annotations.putAll(getPomProperties(baseDir));
        return annotations;
    }

    private Map<String, String> getPomProperties(File baseDir) {
        Map<String, String> annotations = new HashMap<>();
        // lets see if there's a maven generated set of pom properties
        File pomProperties = new File(baseDir, "target/maven-archiver/pom.properties");
        if (pomProperties.isFile()) {
            try {
                Properties properties = new Properties();
                properties.load(new FileInputStream(pomProperties));
                for (Object o : properties.keySet()) {
                    String key = String.valueOf(o);
                    String value = String.valueOf(properties.get(o));
                    if (Strings.isNotNullOrEmpty(key) && Strings.isNotNullOrEmpty(value)) {
                        annotations.put(PROJECT_PREFIX + key, value);
                    }
                }
            } catch (IOException e) {
                logger.get().warn("Failed to load:[ " + pomProperties + "] file to annotate the namespace. Due to: " + e);
            }
        }
            return annotations;
    }

    private String getGitUrl(File basedir)  {
        if (basedir.exists() && basedir.isDirectory()) {
            File gitConfig = new File(basedir, ".git/config");

            if (gitConfig.isFile() && gitConfig.exists()) {

                try (InputStream is = new FileInputStream(gitConfig)) {
                    String text = IOUtil.asString(is);
                    if (text != null) {
                        return getGitUrl(text);
                    }
                } catch (IOException e) {
                    logger.get().warn("Failed to read:[ " + gitConfig + "] file to annotate the namespace. Due to: " + e);
                }
            }
        }
        File parentFile = basedir.getParentFile();
        if (parentFile != null) {
            return getGitUrl(parentFile);
        }
        return null;
    }

    private static File getProjectBaseDir() {
        String basedir = SystemEnvironmentVariables.getPropertyVariable("basedir", ".");
        return new File(basedir);
    }

    private static String getGitUrl(String configText) {
        String remote = null;
        String lastUrl = null;
        String firstUrl = null;
        BufferedReader reader = new BufferedReader(new StringReader(configText));
        Map<String, String> remoteUrls = new HashMap<>();
        while (true) {
            String line = null;
            try {
                line = reader.readLine();
            } catch (IOException e) {
                // ignore should never happen!
            }
            if (line == null) {
                break;
            }
            if (line.startsWith("[remote ")) {
                String[] parts = line.split("\"");
                if (parts.length > 1) {
                    remote = parts[1];
                }
            } else if (line.startsWith("[")) {
                remote = null;
            } else if (remote != null && line.length() > 0 && Character.isWhitespace(line.charAt(0))) {
                String trimmed = line.trim();
                if (trimmed.startsWith("url ")) {
                    String[] parts = trimmed.split("=", 2);
                    if (parts.length > 1) {
                        lastUrl = parts[1].trim();
                        if (firstUrl == null) {
                            firstUrl = lastUrl;
                        }
                        remoteUrls.put(remote, lastUrl);
                    }
                }

            }
        }
        String answer = null;
        if (remoteUrls.size() == 1) {
            return lastUrl;
        } else if (remoteUrls.size() > 1) {
            answer = remoteUrls.get("origin");
            if (answer == null) {
                answer = firstUrl;
            }
        }
        return answer;
    }

}
