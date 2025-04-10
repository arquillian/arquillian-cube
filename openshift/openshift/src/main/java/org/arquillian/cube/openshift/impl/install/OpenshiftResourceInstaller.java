package org.arquillian.cube.openshift.impl.install;

import io.fabric8.kubernetes.api.builder.Visitor;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.client.OpenShiftClient;
import org.arquillian.cube.impl.util.Strings;
import org.arquillian.cube.impl.util.SystemEnvironmentVariables;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.api.Logger;
import org.arquillian.cube.kubernetes.api.ResourceInstaller;
import org.arquillian.cube.kubernetes.impl.install.DefaultResourceInstaller;
import org.arquillian.cube.kubernetes.impl.visitor.CompositeVisitor;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

import static org.arquillian.cube.impl.util.SystemEnvironmentVariables.propertyToEnvironmentVariableName;

public class OpenshiftResourceInstaller extends DefaultResourceInstaller {

    @Deprecated
    private static final String PARAMETERS_FILE = "template.parameters.file";

    @Override
    public ResourceInstaller toImmutable() {
        if (configuration.get() instanceof CubeOpenShiftConfiguration) {
            return new ImmutableOpenshiftResourceInstaller(client.get(), (CubeOpenShiftConfiguration) configuration.get(), logger.get(), new ArrayList<>(serviceLoader.get().all(Visitor.class)));
        } else {
            throw new IllegalStateException("Configuration should be an instance of CubeOpenshiftConfiguration.");
        }
    }

    public static class ImmutableOpenshiftResourceInstaller extends DefaultResourceInstaller.ImmutableResourceInstaller {

        public ImmutableOpenshiftResourceInstaller(KubernetesClient client, CubeOpenShiftConfiguration configuration, Logger logger, List<Visitor> visitors) {
            super(client, configuration, logger, visitors);
        }

        @Override
        public List<HasMetadata> install(URL url) {
            CompositeVisitor compositeVisitor = new CompositeVisitor(visitors);
            if (!client.supports(Project.class)) {
                return super.install(url);
            }

            OpenShiftClient openShiftClient = client.adapt(OpenShiftClient.class);
            try (InputStream is = url.openStream()) {
                KubernetesList list;
                String templateParametersFile = ((CubeOpenShiftConfiguration)configuration).getTemplateParameters();
                //For anyone still using this.
                templateParametersFile = Strings.isNotNullOrEmpty(templateParametersFile) ? templateParametersFile : SystemEnvironmentVariables.getPropertyOrEnvironmentVariable(PARAMETERS_FILE);

                Map<String, String> parameters = createBasicTemplateParameters();

                if (Strings.isNullOrEmpty(templateParametersFile)){
                    logger.warn("Processing template. No parameters file has been specified, processing without external parameters!");
                    list = openShiftClient.templates().load(is).processLocally(parameters);
                } else if (!new File(templateParametersFile).exists()) {
                    throw new IllegalArgumentException("Template parameters file: " + templateParametersFile+" does not exists");
                } else {
                    try (FileInputStream fis = new FileInputStream(templateParametersFile)) {
                        Properties properties = new Properties();
                        properties.load(fis);

                        for (Object k : properties.keySet()) {
                            String s = String.valueOf(k);
                            parameters.put(s, properties.getProperty(s));
                        }
                        logger.info("Processing template, using parameters file:" + templateParametersFile);
                        list = openShiftClient.templates().load(is).processLocally(parameters);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read parameters file!");
                    }
                }
                return openShiftClient.resourceList(list).accept(compositeVisitor).createOrReplace();
            } catch (Throwable t) {
                throw KubernetesClientException.launderThrowable(t);
            }
        }

        private Map<String, String> createBasicTemplateParameters() {
            Map<String, String> env = new HashMap<>();
            env.putAll(System.getenv());
            env.putAll(configuration.getScriptEnvironmentVariables());
            env.put(propertyToEnvironmentVariableName(Configuration.KUBERNETES_NAMESPACE), configuration.getNamespace());
            env.put(propertyToEnvironmentVariableName(Configuration.KUBERNETES_DOMAIN), configuration.getKubernetesDomain());
            env.put(propertyToEnvironmentVariableName(Configuration.KUBERNETES_MASTER),
                configuration.getMasterUrl().toString());
            env.put(propertyToEnvironmentVariableName(Configuration.DOCKER_REGISTY), configuration.getDockerRegistry());
            return env;
        }
    }
}
