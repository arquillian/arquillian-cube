package org.arquillian.cube.openshift.impl.install;

import io.fabric8.kubernetes.api.builder.v3_1.Visitor;
import io.fabric8.kubernetes.api.model.v3_1.HasMetadata;
import io.fabric8.kubernetes.api.model.v3_1.KubernetesList;
import io.fabric8.kubernetes.clnt.v3_1.KubernetesClient;
import io.fabric8.kubernetes.clnt.v3_1.KubernetesClientException;
import io.fabric8.openshift.clnt.v3_1.OpenShiftClient;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.arquillian.cube.impl.util.Strings;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.api.Logger;
import org.arquillian.cube.kubernetes.api.ResourceInstaller;
import org.arquillian.cube.kubernetes.impl.install.DefaultResourceInstaller;
import org.arquillian.cube.kubernetes.impl.visitor.CompositeVisitor;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfiguration;

public class OpenshiftResourceInstaller extends DefaultResourceInstaller {

    @Override
    public ResourceInstaller toImmutable() {
        return new ImmutableOpenshiftResourceInstaller(client.get(), configuration.get(), logger.get(), new ArrayList<>(serviceLoader.get().all(Visitor.class)));
    }

    public static class ImmutableOpenshiftResourceInstaller extends DefaultResourceInstaller.ImmutableResourceInstaller {

        public ImmutableOpenshiftResourceInstaller(KubernetesClient client, Configuration configuration, Logger logger, List<Visitor> visitors) {
            super(client, configuration, logger, visitors);
        }

        @Override
        public List<HasMetadata> install(URL url) {
            CompositeVisitor compositeVisitor = new CompositeVisitor(visitors);
            if (!client.isAdaptable(OpenShiftClient.class)) {
                return super.install(url);
            }

            OpenShiftClient openShiftClient = client.adapt(OpenShiftClient.class);
            try (InputStream is = url.openStream()) {

                KubernetesList list;
                final CubeOpenShiftConfiguration openShiftConfiguration = (CubeOpenShiftConfiguration) this.configuration;
                String templateParametersFile = openShiftConfiguration.getTemplateParametersFile();
                if (Strings.isNullOrEmpty(templateParametersFile)){
                    logger.warn("Processing template. No parameters file has been specified, processing without external parameters!");
                    list = openShiftClient.templates().load(is).processLocally();
                } else if (!new File(templateParametersFile).exists()) {
                    throw new IllegalArgumentException("Template parameters file: " + templateParametersFile+" does not exists");
                } else {
                    HashMap<String, String> map = new HashMap<>();
                    try (FileInputStream fis = new FileInputStream(templateParametersFile)) {
                        Properties properties = new Properties();
                        properties.load(fis);

                        for (Object k : properties.keySet()) {
                            String s = String.valueOf(k);
                            map.put(s, properties.getProperty(s));
                        }
                        logger.info("Processing template, using parameters file:" + templateParametersFile);
                        list = openShiftClient.templates().load(is).processLocally(map);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read parameters file!");
                    }
                }
                return openShiftClient.resourceList(list).accept(compositeVisitor).createOrReplace();
            } catch (Throwable t) {
                throw KubernetesClientException.launderThrowable(t);
            }
        }
    }
}
