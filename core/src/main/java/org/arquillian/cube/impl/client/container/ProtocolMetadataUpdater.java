package org.arquillian.cube.impl.client.container;

import org.arquillian.cube.impl.util.ContainerUtil;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.arquillian.cube.spi.metadata.HasPortBindings;
import org.arquillian.cube.spi.metadata.HasPortBindings.PortAddress;
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

        try {
            Cube<?> cube = registry.getCube(ContainerUtil.getCubeIDForContainer(container));
            if(cube == null) {
                return;
            }
            HasPortBindings portBindings = cube.getMetadata(HasPortBindings.class);
            if (portBindings == null) {
                return;
            }
            for(Object contextObj : originalMetaData.getContexts()) {
                if(contextObj instanceof HTTPContext) {
                    HTTPContext context = (HTTPContext)contextObj;
                    String ip = context.getHost();
                    int port = context.getPort();
                    final PortAddress mappedPort = portBindings.getMappedAddress(port);
                    final String bindingIp;
                    final Integer bindingPort;
                    if (mappedPort != null) {
                        bindingIp = mappedPort.getIP();
                        bindingPort = mappedPort.getPort();
                    } else {
                        continue;
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
        } finally {
            if(updated) {
                protocolMetaDataProducer.set(updatedMetaData);
            } else {
                eventContext.proceed();
            }
        }

    }
}
