package org.arquillian.cube.openshift.impl.install;

import org.arquillian.cube.impl.util.SystemEnvironmentVariables;
import org.arquillian.cube.kubernetes.impl.install.DefaultResourceInstaller;
import org.arquillian.cube.kubernetes.impl.visitor.CompositeVisitor;
import org.jboss.arquillian.core.spi.ServiceLoader;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import io.fabric8.kubernetes.api.builder.Visitor;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.client.OpenShiftClient;

public class OpenshiftResourceInstaller extends DefaultResourceInstaller {

    private static final String PARAMETERS_FILE = "template.parameters.file";

    @Override
    public List<HasMetadata> install(URL url) {
        ServiceLoader serviceLoader = this.serviceLoader.get();
        KubernetesClient client = this.client.get();
        List<Visitor> visitors = new ArrayList<>(serviceLoader.all(Visitor.class));
        CompositeVisitor compositeVisitor = new CompositeVisitor(visitors);

        if (!client.isAdaptable(OpenShiftClient.class)) {
            return super.install(url);
        }

        OpenShiftClient openShiftClient = client.adapt(OpenShiftClient.class);

        try (InputStream is = url.openStream()) {
            KubernetesList list;
            String templateParametersFile = SystemEnvironmentVariables.getPropertyOrEnvironmentVariable(PARAMETERS_FILE);
            if (templateParametersFile == null || !(new File(templateParametersFile).exists())) {
                list = openShiftClient.templates().load(is).processLocally(new File(templateParametersFile));
            } else {
                list = openShiftClient.templates().load(is).process();
            }
            return openShiftClient.resourceList(list).accept(compositeVisitor).createOrReplace();
        } catch (Throwable t) {
            throw KubernetesClientException.launderThrowable(t);
        }
    }
}
