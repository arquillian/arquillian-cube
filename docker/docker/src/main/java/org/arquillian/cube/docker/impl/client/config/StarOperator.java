package org.arquillian.cube.docker.impl.client.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class StarOperator {

    private static Random random = new Random();

    private StarOperator(){
    }

    public static void adaptPortBindingToParallelRun(CubeContainer cubeContainer) {
        final Collection<PortBinding> portBindings = cubeContainer.getPortBindings();
        if (portBindings == null) {
            return;
        }
        for (PortBinding portBinding : portBindings) {
            final int randomPrivatePort = generateRandomPrivatePort();
            portBinding.setBound(randomPrivatePort);
        }
    }

    public static void adaptLinksToParallelRun(UUID uuid, CubeContainer cubeContainer) {
        final Collection<Link> links = cubeContainer.getLinks();

        if (links == null) {
            return;
        }

        for (Link link : links) {
            if (link.getName().endsWith("*")) {
                String linkTemplate = link.getName().substring(0, link.getName().lastIndexOf('*'));
                link.setName(generateNewName(linkTemplate, uuid));

                String environmentVariable = linkTemplate.toUpperCase() + "_HOSTNAME=" + link.getName();
                if (link.isAliasSet()) {
                    link.setAlias(generateNewName(link.getAlias(), uuid));
                    environmentVariable = linkTemplate.toUpperCase() + "_HOSTNAME=" + link.getAlias();
                }

                final Collection<String> env = cubeContainer.getEnv();
                if (env != null) {
                    // to avoid duplicates
                    if (env.contains(environmentVariable)) {
                        env.remove(environmentVariable);
                    }
                } else {
                    cubeContainer.setEnv(new ArrayList<>());
                }
                cubeContainer.getEnv().add(environmentVariable);
            }
        }
    }

    public static void adaptDependenciesToParallelRun(UUID uuid, CubeContainer cubeContainer) {
        final Collection<String> dependencies = cubeContainer.getDependsOn();

        if (dependencies == null) {
            return;
        }
        
        ArrayList<String> adjustedDependsOn = new ArrayList<>();

        for (String dependency : dependencies) {
            if (dependency.endsWith("*")) {
                String dependencyTemplate = dependency.substring(0,dependency.lastIndexOf('*'));
                adjustedDependsOn.add(generateNewName(dependencyTemplate, uuid));
            }else {
                adjustedDependsOn.add(dependency);
            }
        }
        
        cubeContainer.setDependsOn(adjustedDependsOn);
    }

    public static void adaptNetworksToParalledRun(Map<String,String> networkResolutions, CubeContainer cubeContainer) {
        String networkMode = cubeContainer.getNetworkMode();
        if (networkMode != null && networkResolutions.containsKey(networkMode)) {
            cubeContainer.setNetworkMode(networkResolutions.get(networkMode));
        }

        if (cubeContainer.getNetworks() != null) {
            ArrayList<String> networks = new ArrayList<>();
            for (String network : cubeContainer.getNetworks()) {
                if (networkResolutions.containsKey(network)) {
                    networks.add(networkResolutions.get(network));
                } else {
                    networks.add(network);
                }
            }
            cubeContainer.setNetworks(networks);
        }
    }

    public static String generateNewName(String containerName, UUID uuid) {
        return containerName + "_" + uuid;
    }

    public static int generateRandomPrivatePort() {
        final int randomPort = random.nextInt(16383);
        return randomPort + 49152;
    }
}
