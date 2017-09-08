package org.arquillian.cube.openshift.impl.dns;

import io.fabric8.openshift.api.model.v2_6.RouteList;
import sun.net.spi.nameservice.NameService;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * CENameService/ArqCubeNameService
 *
 * @author Rob Cernich
 * @author fspolti
 */
public class ArqCubeNameService implements NameService {

    private static final Set<String> hosts = new HashSet<>();
    private static InetAddress routerAddr;

    public static void setRoutes(RouteList routeList, String routerHost) {
        synchronized (hosts) {
            hosts.clear();
            try {
                ArqCubeNameService.routerAddr = routerHost == null ? null : InetAddress.getByName(routerHost);
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException("Invalid IP for router host", e);
            }

            routeList.getItems().stream().filter(Objects::nonNull)
                .forEach(route -> {
                    System.out.println(String.format("Adding route to Arquillian Naming Service: %s %s", routerHost, route.getSpec().getHost()));
                    hosts.add(route.getSpec().getHost());
                });
        }
    }

    @Override
    public InetAddress[] lookupAllHostAddr(String host) throws UnknownHostException {
        synchronized (hosts) {
            if (routerAddr != null && hosts.contains(host)) {
                return new InetAddress[] {InetAddress.getByAddress(host, routerAddr.getAddress()) };
            }
            throw new UnknownHostException(host);
        }
    }

    @Override
    public String getHostByAddr(byte[] addr) throws UnknownHostException {
        throw new UnknownHostException();
    }
}
