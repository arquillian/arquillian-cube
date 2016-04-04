package org.arquillian.cube.docker.impl.model;

import org.arquillian.cube.docker.impl.util.OperatingSystemFamily;
import org.arquillian.cube.docker.impl.util.OperatingSystemResolver;

public enum DockerMachineDistro {

    LINUX(new OperatingSystemFamily[] {OperatingSystemFamily.LINUX}, "docker-machine-Linux-x86_64"),
    OSX(new OperatingSystemFamily[] {OperatingSystemFamily.MAC}, "docker-machine-Darwin-x86_64"),
    WIN_64(new OperatingSystemFamily[] {OperatingSystemFamily.WINDOWS}, "docker-machine-Windows-x86_64.exe");

    private OperatingSystemFamily[] osFamily;

    private String distro;

    DockerMachineDistro(OperatingSystemFamily[] osFamily, String distro) {
        this.osFamily = osFamily;
        this.distro = distro;
    }

    public static String resolveDistro() {
        OperatingSystemFamily currentOSFamily = new OperatingSystemResolver().currentOperatingSystem().getFamily();
        for (DockerMachineDistro distro : values()) {
            for (OperatingSystemFamily osFamily : distro.osFamily) {
                if (osFamily == currentOSFamily) {
                    return distro.distro;
                }
            }
        }
        return null;
    }

}
