package org.arquillian.cube.openshift.impl.client;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;

import io.fabric8.kubernetes.api.Kubernetes;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;

public class GitServer {

    private static int PORT = 6768;

    private Kubernetes client;
    private String namespace;
    private Pod server;

    public GitServer(Kubernetes client, String namespace) {
        this.client = client;
        this.namespace = namespace;
    }

    public URI push(File source, String name) throws Exception {
        init();

        URI server = getServerURI();

        String id = name;
        String repoUrl = server.toASCIIString() + "/" + id;

        File gitRoot = new File("target", id);
        FileUtils.copyDirectory(source, gitRoot);

        Git git = Git.init().setDirectory(gitRoot).call();
        Repository repo = git.getRepository();
        repo.getConfig().setString("remote", "origin", "url", repoUrl);
        repo.getConfig().save();

        git.add().addFilepattern(".").call();
        git.commit().setMessage("testing").setAuthor("Arquillian", "cube@discuss.arquillian.org").call();
        git.push().setRemote("origin").setPushAll().setForce(true).call();
        repo.close();

        return URI.create(repoUrl);

    }

    public void shutdown() throws Exception {
        if (server != null) {
            client.deletePod(server.getMetadata().getName(), namespace);
            client.deleteSecret("gitserver-config", namespace);
        }
    }

    private void init() throws Exception {
        if (server == null) {
            server = getSpec();

            try {
                server = client.getPod(server.getMetadata().getName(), namespace);
            } catch (WebApplicationException e) { // probably 404
                Object response = KubernetesHelper.loadJson(client.createPod(server, namespace));
                if (response instanceof Pod) {
                    server = (Pod) response;
                } else {
                    throw new RuntimeException("Unknown response on gitserver deploy: " + server);
                }
            }
            server = ResourceUtil.waitForStart(client, server);
        }
    }

    private URI getServerURI() {
        return URI.create("http://" + server.getStatus().getHostIP() + ":" + PORT);
    }

	private Pod getSpec() {
		Map<String, String> labels = new HashMap<String, String>();
		labels.put("generatedby", "arquillian");

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
						.withHostPort(PORT)
						.withContainerPort(8080)
						.endPort()
					.addNewEnv()
						.withName("GIT_HOME")
						.withValue("/var/lib/git")
						.endEnv()
				.endContainer()
				.endSpec()
			.build();
	}
}
