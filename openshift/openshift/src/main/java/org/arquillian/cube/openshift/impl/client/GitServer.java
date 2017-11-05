package org.arquillian.cube.openshift.impl.client;

import io.fabric8.kubernetes.api.model.v2_6.Pod;
import io.fabric8.kubernetes.api.model.v2_6.PodBuilder;
import io.fabric8.kubernetes.api.model.v2_6.Service;
import io.fabric8.kubernetes.api.model.v2_6.ServiceBuilder;
import io.fabric8.kubernetes.clnt.v2_6.Config;
import io.fabric8.openshift.clnt.v2_6.NamespacedOpenShiftClient;
import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.arquillian.cube.kubernetes.impl.portforward.PortForwarder;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;

public class GitServer {
    private static final String GIT_SERVICE = "git";
    private static final String GIT_LOCALPORT = "10001";
    private static final String GIT_REMOTEPORT = "8080";
    private NamespacedOpenShiftClient client;
    private String namespace;
    private Pod server;
    private PortForwarder forwarder;
    private Config config;
    private Service service;

    public GitServer(NamespacedOpenShiftClient client, Config config, String namespace) {
        this.client = client;
        this.config = config;
        this.namespace = namespace;
    }

    public URI push(File source, String name) throws Exception {
        init();

        File gitRoot = new File("target", name);
        FileUtils.copyDirectory(source, gitRoot);

        // Push via port forward
        String repoUrl = String.format("http://localhost:%s/%s", GIT_LOCALPORT, name);
        Git git = Git.init().setDirectory(gitRoot).call();
        Repository repo = git.getRepository();
        repo.getConfig().setString("remote", "origin", "url", repoUrl);
        repo.getConfig().save();

        git.add().addFilepattern(".").call();
        git.commit().setMessage("testing").setAuthor("Arquillian", "cube@discuss.arquillian.org").call();
        git.push().setRemote("origin").setPushAll().setForce(true).call();
        repo.close();

        // Return an internal service name, for use within the openshift network
        String serverUrl = String.format("http://%s:%s/%s", GIT_SERVICE, GIT_REMOTEPORT, name);
        return URI.create(serverUrl);
    }

    public void shutdown() throws Exception {
        if (forwarder != null) {
            forwarder.close();
        }
        if (service != null) {
            client.services().inNamespace(namespace).withName(service.getMetadata().getName()).delete();
        }
        if (server != null) {
            client.pods().inNamespace(namespace).withName(server.getMetadata().getName()).delete();
            client.secrets().inNamespace(namespace).withName("gitserver-config").delete();
        }
    }

    private void init() throws Exception {
        createService();

        if (server == null) {
            server = getSpec();

            server = client.pods().inNamespace(namespace).withName(server.getMetadata().getName()).get();
            if (server == null) {
                server = client.pods().inNamespace(namespace).create(getSpec());
                server = ResourceUtil.waitForStart(client, server);
            }
        }
        if (forwarder != null) {
            forwarder.close();
        }
        forwarder = new PortForwarder(config, server.getMetadata().getName());
        forwarder.forwardPort(Integer.valueOf(GIT_LOCALPORT), Integer.valueOf(GIT_REMOTEPORT));
    }

    private Pod getSpec() {
        Map<String, String> labels = new HashMap<String, String>();
        labels.put("generatedby", "arquillian");
        labels.put("pod", "arquillian-gitserver");

        return new PodBuilder()
            .withNewMetadata()
            .withName("arquillian-gitserver")
            .withLabels(labels)
            .endMetadata()
            .withNewSpec()
            .addNewContainer()
            .withName("arquillian-gitserver")
            .withImage("aslakknutsen/openshift-arquillian-gitserver")
            .addNewPort()
            .withContainerPort(Integer.valueOf(GIT_REMOTEPORT))
            .endPort()
            .addNewEnv()
            .withName("GIT_HOME")
            .withValue("/var/lib/git")
            .endEnv()
            // This volume is necessary in order to override the image volume, otherwise
            // we wouldn't have permission to write in that directory
            .addNewVolumeMount()
            .withName("git-repo")
            .withMountPath("/var/lib/git")
            .withReadOnly(false)
            .endVolumeMount()
            .endContainer()
            .addNewVolume()
            .withName("git-repo")
            .withNewEmptyDir("Memory")
            .endVolume()
            .endSpec()
            .build();
    }

    private void createService() {
        if (service != null) {
            return;
        }

        Map<String, String> labels = new HashMap<String, String>();
        labels.put("generatedby", "arquillian");

        Service svc = client.services().inNamespace(namespace).withName(GIT_SERVICE).get();
        if (svc == null) {
            svc = new ServiceBuilder()
                .withNewMetadata()
                .withName(GIT_SERVICE)
                .withLabels(labels)
                .endMetadata()
                .withNewSpec()
                .addNewPort()
                .withPort(Integer.valueOf(GIT_REMOTEPORT))
                .withNewTargetPort(Integer.valueOf(GIT_REMOTEPORT))
                .endPort()
                .addToSelector("pod", "arquillian-gitserver")
                .and()
                .build();
            client.services().inNamespace(namespace).create(svc);
        }
        service = svc;
    }
}
