package org.arquillian.cube.impl.client.container;

import org.arquillian.cube.spi.Binding;
import org.arquillian.cube.spi.Binding.PortBinding;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
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
            Binding binding = cube.bindings();
            String gatewayIp = cube.bindings().getIP();
            for(Object contextObj : originalMetaData.getContexts()) {
                if(contextObj instanceof HTTPContext) {
                    HTTPContext context = (HTTPContext)contextObj;
                    PortBinding mapped = binding.getBindingForExposedPort(context.getPort());
                    String ip = context.getHost();
                    int port = context.getPort();
                    if(mapped != null && port != mapped.getBindingPort()) {
                        updated = true;
                        port = mapped.getBindingPort();
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
}
