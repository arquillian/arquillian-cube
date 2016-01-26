package org.arquillian.cube.impl.client.container;

import org.arquillian.cube.spi.Binding;
import org.arquillian.cube.spi.Binding.PortBinding;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.metadata.HasMappedPorts;
import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.arquillian.container.spi.context.annotation.DeploymentScoped;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.EventContext;

public class ProtocolMetadataUpdater {

    @Inject @DeploymentScoped
    private InstanceProducer<ProtocolMetaData> protocolMetaDataProducer;

    public void update(@Observes EventContext<ProtocolMetaData> eventContext, Container container, CubeRegistry registry) {

        ProtocolMetaData originalMetaData = eventContext.getEvent();
        ProtocolMetaData updatedMetaData = new ProtocolMetaData();
        boolean updated = false;

        Cube<?> cube = registry.getCube(container.getName());
        if(cube != null) {
            HasMappedPorts mappedPorts = cube.getMetadata(HasMappedPorts.class);
            Binding binding = cube.configuredBindings();
            String gatewayIp = binding.getIP();
            for(Object contextObj : originalMetaData.getContexts()) {
                if(contextObj instanceof HTTPContext) {
                    HTTPContext context = (HTTPContext)contextObj;
                    String ip = context.getHost();
                    int port = context.getPort();
                    final String bindingIp;
                    final Integer bindingPort;
                    if (mappedPorts != null && mappedPorts.forTargetPort(port) != null) {
                        bindingIp = mappedPorts.getIP();
                        bindingPort = mappedPorts.forTargetPort(port);
                    } else {
                        PortBinding mapped = binding.getBindingForExposedPort(context.getPort());
                        if (mapped != null) {
                            bindingIp = gatewayIp;
                            bindingPort = mapped.getBindingPort();
                        } else {
                            continue;
                        }
                    }
                    if(bindingPort != null && port != bindingPort) {
                        updated = true;
                        port = bindingPort;
                    }
                    if(bindingIp != null && !bindingIp.equals(ip)) {
                        updated = true;
                        ip = bindingIp;
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
}
