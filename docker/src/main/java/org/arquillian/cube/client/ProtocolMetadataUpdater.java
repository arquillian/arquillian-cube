package org.arquillian.cube.client;

import java.util.HashMap;
import java.util.Map;

import org.arquillian.cube.docker.DockerClientExecutor;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.arquillian.container.spi.context.annotation.DeploymentScoped;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.EventContext;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.HostConfig;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;

public class ProtocolMetadataUpdater {

    @Inject @DeploymentScoped
    private InstanceProducer<ProtocolMetaData> protocolMetaDataProducer;

    public void update(@Observes EventContext<ProtocolMetaData> eventContext, DockerClientExecutor executor, ContainerMapping containerMapping, Container container) {

        ProtocolMetaData originalMetaData = eventContext.getEvent();
        ProtocolMetaData updatedMetaData = new ProtocolMetaData();
        boolean updated = false;

        String containerId = containerMapping.getContainerByName(container.getName());
        if(containerId != null) {
            InspectContainerResponse inspectResponse = inspect(executor.getDockerClient(), containerId);
            HostConfig hostConfig = inspectResponse.getHostConfig();
            String gatewayIp = inspectResponse.getNetworkSettings().getGateway();
            Map<Integer, Integer> portMapping = getPortMappings(hostConfig.getPortBindings());
            for(Object contextObj : originalMetaData.getContexts()) {
                if(contextObj instanceof HTTPContext) {
                    HTTPContext context = (HTTPContext)contextObj;
                    Integer mapped = portMapping.get(context.getPort());
                    String ip = context.getHost();
                    int port = context.getPort();
                    if(mapped != null && port != mapped) {
                        updated = true;
                        port = mapped;
                    }
                    if(!gatewayIp.equals(ip)) {
                        updated = true;
                        ip = gatewayIp;
                    }
                    if(updated) {
                        HTTPContext newContext = new HTTPContext(ip, port);
                        for(Servlet servlet : context.getServlets()) {
                            newContext.add(servlet);
                        }
                        updatedMetaData.addContext(newContext);
                    }

                } else {
                    updatedMetaData.addContext(contextObj);
                }
            }
        }

        if(updated) {
            protocolMetaDataProducer.set(updatedMetaData);
        } else {
            eventContext.proceed();
        }
    }

    private Map<Integer, Integer> getPortMappings(Ports ports) {
        Map<Integer, Integer> result = new HashMap<Integer, Integer>();

        for (Map.Entry<ExposedPort, Binding> binding : ports.getBindings().entrySet()) {
            result.put(binding.getKey().getPort(), binding.getValue().getHostPort());
        }
        return result;
    }

    // TODO: Inspect happens twice, once here and once in pooling await strategy. Reuse somehow?
    private InspectContainerResponse inspect(DockerClient client, String containerId) {
        return client.inspectContainerCmd(containerId).exec();
    }
}
