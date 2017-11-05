package org.arquillian.cube.kubernetes.impl;

import java.net.Inet4Address;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class PortForwarderTest {

    private final String[] portsToForwardStringBackward = {"test:1234", "test:9999", "test:9990", "test:8080"};
    private final String[] portsToForwardString = {"test:0:1234", "test:0:9999", "test:0:9990", "test:0:8080"};
    private final String[] useSamePortsToForwardString = {"test::1234", "test::9999", "test::9990", "test::8080"};
    private Set<String> portsToForward = new HashSet<String>();
    private Map<Integer, Integer> proxiedPorts = new HashMap<>();

    @Test
    public void testAlleatoryPortBackwardCapabilities() {
        resolvePorts("aleatoryPortBackward");
        for (Map.Entry<Integer, Integer> entry : proxiedPorts.entrySet()) {
            //key = containerPort, value = mappedPort
            Assert.assertNotEquals(entry.getKey(), entry.getValue());
        }
    }

    @Test
    public void testAlleatoryPort() {
        resolvePorts("aleatoryPort");
        for (Map.Entry<Integer, Integer> entry : proxiedPorts.entrySet()) {
            //key = containerPort, value = mappedPort
            Assert.assertNotEquals(entry.getKey(), entry.getValue());
        }
    }

    @Test
    public void testSamePort() {
        resolvePorts("samePort");
        for (Map.Entry<Integer, Integer> entry : proxiedPorts.entrySet()) {
            //key = containerPort, value = mappedPort
            Assert.assertEquals(entry.getKey(), entry.getValue());
        }
    }

    @Test
    public void testSameAndAleatoryPort() {

        //Test1 pod:port
        portsToForward.add("test:8080");
        resolvePorts("none");
        for (Map.Entry<Integer, Integer> entry : proxiedPorts.entrySet()) {
            //key = containerPort, value = mappedPort
            Assert.assertNotEquals(entry.getKey(), entry.getValue());
        }

        //Test2 pod::port
        portsToForward.add("test::8180");
        resolvePorts("none");
        for (Map.Entry<Integer, Integer> entry : proxiedPorts.entrySet()) {
            //key = containerPort, value = mappedPort
            Assert.assertEquals(entry.getKey(), entry.getValue());
        }

        //Test3 pod:0:port
        portsToForward.add("test:0:8280");
        resolvePorts("none");
        for (Map.Entry<Integer, Integer> entry : proxiedPorts.entrySet()) {
            //key = containerPort, value = mappedPort
            Assert.assertNotEquals(entry.getKey(), entry.getValue());
        }

        //Test4 pod:port:port
        portsToForward.add("test:8380:8380");
        resolvePorts("none");
        for (Map.Entry<Integer, Integer> entry : proxiedPorts.entrySet()) {
            //key = containerPort, value = mappedPort
            Assert.assertEquals(entry.getKey(), entry.getValue());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testNegativePort() {
        //test should fail, let's try to map a invalid port
        Assert.assertEquals(-1, allocateLocalPort(-1));
    }

    private void resolvePorts(String method) {
        //cleaning previous ports
        portsToForward = new HashSet<String>();

        if ("aleatoryPortBackward".equals(method)) {
            for (int i = 0; i < portsToForwardStringBackward.length; i++) {
                portsToForward.add(portsToForwardStringBackward[i]);
            }
        } else if ("aleatoryPort".equals(method)) {
            for (int i = 0; i < portsToForwardString.length; i++) {
                portsToForward.add(portsToForwardString[i]);
            }
        } else if ("samePort".equals(method)) {
            for (int i = 0; i < useSamePortsToForwardString.length; i++) {
                portsToForward.add(useSamePortsToForwardString[i]);
            }
        }
        // Syntax: pod:port - backward compatibility
        // pod::port - use the same port than container port
        // pod:0:port - use an aleatory port
        for (String proxy : portsToForward) {
            String[] split = proxy.split(":");
            if (split.length == 2 || split.length == 3) {
                final int containerPort = !"".equals(split[1]) ? Integer.valueOf(split[1]) : Integer.valueOf(split[2]);
                int mappedPort = 0;
                if (split.length == 3 && "".equals(split[1])) {
                    // pod::port - use the same port than container port
                    mappedPort = allocateLocalPort(Integer.valueOf(split[2]));
                } else if (split.length == 3 && "0".equals(split[1])) {
                    //pod:0:port - use an aleatory port or pod:port:port to map the same port
                    mappedPort = allocateLocalPort(Integer.valueOf(split[1]));
                } else {
                    // pod:port - backward compatibility, aleatory port
                    mappedPort = allocateLocalPort(0);
                }
                proxiedPorts.put(containerPort, mappedPort);
            }
        }
    }

    //the original method is inside a private inner class, so I am replicating it here.
    private int allocateLocalPort(int port) {
        try {
            try (ServerSocket serverSocket = new ServerSocket(port, 0, Inet4Address.getLocalHost())) {
                return serverSocket.getLocalPort();
            }
        } catch (Throwable t) {
            throw new IllegalStateException("Could not allocate local port for forwarding proxy", t);
        }
    }
}
