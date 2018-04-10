package org.arquillian.cube.kubernetes.impl.utils;

import org.arquillian.cube.kubernetes.impl.SessionManager;

public class MavenUtils {

    public static boolean isRunningFromMaven() {
        try {
            SessionManager.class.getClassLoader().loadClass("org.apache.maven.surefire.booter.ForkedBooter");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
