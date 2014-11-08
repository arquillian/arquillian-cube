package org.arquillian.cube.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.arquillian.container.spi.context.annotation.DeploymentScoped;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.EventContext;

public class ProtocolMetadataUpdater {

    private static final String EXPOSED_PORT = "exposedPort";
    private static final String PORT = "port";
    private static final String PORT_BINDINGS = "portBindings";

    @Inject @DeploymentScoped
    private InstanceProducer<ProtocolMetaData> protocolMetaDataProducer;

    @Inject
    private Instance<CubeConfiguration> configInst;

    @Inject
    private Instance<Container> containerInst;

    public void update(@Observes EventContext<ProtocolMetaData> eventContext) {

        ProtocolMetaData originalMetaData = eventContext.getEvent();
        ProtocolMetaData updatedMetaData = new ProtocolMetaData();
        boolean updated = false;

        Map<Integer, Integer> portMapping = getPortMappings();
        for(Object contextObj : originalMetaData.getContexts()) {
            if(contextObj instanceof HTTPContext) {
                HTTPContext context = (HTTPContext)contextObj;
                Integer mapped = portMapping.get(context.getPort());
                if(mapped != null && mapped != context.getPort()) {
                    updated = true;
                    HTTPContext newContext = new HTTPContext(context.getHost(), mapped);
                    for(Servlet servlet : context.getServlets()) {
                        newContext.add(servlet);
                    }
                    updatedMetaData.addContext(newContext);
                }

            } else {
                updatedMetaData.addContext(contextObj);
            }
        }

        if(updated) {
            protocolMetaDataProducer.set(updatedMetaData);
        } else {
            eventContext.proceed();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Integer> getPortMappings() {
        Map<Integer, Integer> result = new HashMap<Integer, Integer>();

        CubeConfiguration config = configInst.get();
        Container container = containerInst.get();

        if(config == null || container == null) {
            return result;
        }

        Map<String, Object> containerConfig = (Map<String, Object>)config.getDockerContainersContent().get(container.getName());
        List<Object> bindings = (List<Object>)containerConfig.get(PORT_BINDINGS);
        for(Object bindingObj: bindings) {
            Map<String, Object> binding = (Map<String, Object>)bindingObj;
            Integer port = (Integer)binding.get(PORT);
            String exposedPort = (String)binding.get(EXPOSED_PORT);
            if(exposedPort.indexOf("/") != -1) {
                exposedPort = exposedPort.substring(0, exposedPort.indexOf("/"));
            }
            result.put(Integer.parseInt(exposedPort), port);
        }

        return result;
    }
}
