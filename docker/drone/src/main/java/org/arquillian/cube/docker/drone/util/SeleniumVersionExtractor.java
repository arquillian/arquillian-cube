package org.arquillian.cube.docker.drone.util;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for Selenium.
 */
public class SeleniumVersionExtractor {

    private static final Logger logger = Logger.getLogger(SeleniumVersionExtractor.class.getName());
    private static final String SELENIUM_VERSION = "latest";

    /**
     * Returns current selenium version from JAR set in classpath.
     * @return Version of Selenium.
     */
    public static String fromClassPath() {
        Set<String> versions = new HashSet<>();
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> manifests = classLoader.getResources("META-INF/MANIFEST.MF");

            while (manifests.hasMoreElements()) {
                URL manifestURL = manifests.nextElement();
                try (InputStream is = manifestURL.openStream()) {
                    Manifest manifest = new Manifest();
                    manifest.read(is);

                    Attributes buildInfo = manifest.getAttributes("Build-Info");
                    if (buildInfo != null) {
                        if (buildInfo.getValue("Selenium-Version") != null) {
                            versions.add(buildInfo.getValue("Selenium-Version"));
                        } else {
                            // might be in build-info part
                            if (manifest.getEntries() != null) {
                                if (manifest.getEntries().containsKey("Build-Info")) {
                                    final Attributes attributes = manifest.getEntries().get("Build-Info");

                                    if (attributes.getValue("Selenium-Version") != null) {
                                        versions.add(attributes.getValue("Selenium-Version"));
                                    }
                                }
                            }
                        }
                    }
                }

            }

        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception {0} occurred while resolving selenium version and latest image is going to be used.", e.getMessage());
            return SELENIUM_VERSION;
        }

        if (versions.isEmpty()) {
            logger.log(Level.INFO, "No version of Selenium found in classpath. Using latest image.");
            return SELENIUM_VERSION;
        }

        String foundVersion = versions.iterator().next();
        if (versions.size() > 1) {
            logger.log(Level.WARNING, "Multiple versions of Selenium found in classpath. Using the first one found {0}.", foundVersion);
        }

        return foundVersion;
    }


}
