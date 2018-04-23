package org.arquillian.cube.docker.impl.util;

import static org.arquillian.cube.docker.impl.util.OperatingSystemFamily.DEC_OS;
import static org.arquillian.cube.docker.impl.util.OperatingSystemFamily.LINUX;
import static org.arquillian.cube.docker.impl.util.OperatingSystemFamily.MAC;
import static org.arquillian.cube.docker.impl.util.OperatingSystemFamily.UNIX;
import static org.arquillian.cube.docker.impl.util.OperatingSystemFamily.UNKNOWN;
import static org.arquillian.cube.docker.impl.util.OperatingSystemFamily.WINDOWS;

public enum OperatingSystem implements OperatingSystemInterface {
    LINUX_OS("Linux", LINUX),
    MAC_OSX("Mac OS X", MAC),
    MAC_OS("Mac OS", MAC),
    WINDOWS_95("Windows 95", WINDOWS),
    WINDOWS_98("Windows 98", WINDOWS),
    WINDOWS_ME("Windows Me", WINDOWS),
    WINDOWS_NT("Windows NT", WINDOWS),
    WINDOWS_2000("Windows 2000", WINDOWS),
    WINDOWS_XP("Windows XP", WINDOWS),
    WINDOWS_7("Windows 7", WINDOWS),
    WINDOWS_8("Windows 8", WINDOWS),
    WINDOWS_10("Windows 10", WINDOWS),
    WINDOWS_2003("Windows 2003", WINDOWS),
    WINDOWS_2008("Windows 2008", WINDOWS),
    SUN_OS("Sun OS ", UNIX),
    MPE_IX("MPE/iX", UNIX),
    HP_UX("HP-UX", UNIX),
    AIX("AIX", UNIX),
    OS_390("OS/390", UNIX),
    FREEBSD("FreeBSD", UNIX),
    IRIX("Irix", UNIX),
    DIGITAL_UNIX("Digital Unix", UNIX),
    NETWARE_4_11("NetWare 4.11", UNIX),
    OSF1("OSF1", UNIX),
    OPENVMS("OpenVMS", DEC_OS),
    UNKNOWN_OS("Unknown", UNKNOWN);

    final private String label;
    final private OperatingSystemFamily family;
    final private OperatingSystemFamily default_family;

    private OperatingSystem(String label, OperatingSystemFamily family) {
        this.label = label;
        this.family = family;
        if (label.startsWith("Windows")) {
            this.default_family = OperatingSystemFamily.WINDOWS_NPIPE;
        } else {
            this.default_family = OperatingSystemFamily.UNIX;
        }
    }

    static public OperatingSystem resolve(String osName) {
        for (OperatingSystem os : OperatingSystem.values()) {
            if (os.label.equalsIgnoreCase(osName)) return os;
        }
        return OperatingSystem.UNKNOWN_OS;
    }

    public String getLabel() {
        return label;
    }

    public OperatingSystemFamily getFamily() {
        return family;
    }

    public OperatingSystemFamily getDefaultFamily() {
        return default_family;
    }

}
