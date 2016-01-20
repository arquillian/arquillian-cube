package org.arquillian.cube.docker.impl.model;

import org.arquillian.cube.docker.impl.util.OperatingSystemFamily;
import org.arquillian.cube.docker.impl.util.OperatingSystemResolver;

public enum DockerMachineDistro {

    LINUX(new OperatingSystemFamily[] {OperatingSystemFamily.LINUX}, "docker-machine_linux-amd64"),
    OSX(new OperatingSystemFamily[] {OperatingSystemFamily.MAC}, "docker-machine_darwin-amd64"),
    //WIN_32(new OperatingSystemFamily[] {OperatingSystemFamily.WINDOWS}, "docker-machine_windows-386.exe"),
    WIN_64(new OperatingSystemFamily[] {OperatingSystemFamily.WINDOWS}, "docker-machine_windows-amd64.exe");

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
