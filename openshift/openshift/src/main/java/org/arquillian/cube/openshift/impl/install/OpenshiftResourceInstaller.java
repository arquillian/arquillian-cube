package org.arquillian.cube.openshift.impl.install;

import io.fabric8.kubernetes.api.builder.v2_6.Visitor;
import io.fabric8.kubernetes.api.model.v2_6.HasMetadata;
import io.fabric8.kubernetes.api.model.v2_6.KubernetesList;
import io.fabric8.kubernetes.clnt.v2_6.KubernetesClient;
import io.fabric8.kubernetes.clnt.v2_6.KubernetesClientException;
import io.fabric8.openshift.clnt.v2_6.OpenShiftClient;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.arquillian.cube.impl.util.SystemEnvironmentVariables;
import org.arquillian.cube.kubernetes.impl.install.DefaultResourceInstaller;
import org.arquillian.cube.kubernetes.impl.visitor.CompositeVisitor;
import org.jboss.arquillian.core.spi.ServiceLoader;

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
            if (templateParametersFile != null && new File(templateParametersFile).exists()) {
                list = openShiftClient.templates().load(is).processLocally(new File(templateParametersFile));
            } else {
                list = openShiftClient.templates().load(is).processLocally();
            }
            return openShiftClient.resourceList(list).accept(compositeVisitor).createOrReplace();
        } catch (Throwable t) {
            throw KubernetesClientException.launderThrowable(t);
        }
    }
}
