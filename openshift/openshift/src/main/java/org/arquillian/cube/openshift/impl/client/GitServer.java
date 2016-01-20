package org.arquillian.cube.openshift.impl.client;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;

public class GitServer {

    private static int PORT = 6768;

    private KubernetesClient client;
    private String namespace;
    private Pod server;

    public GitServer(KubernetesClient client, String namespace) {
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
            client.pods().inNamespace(namespace).withName(server.getMetadata().getName()).delete();
            client.secrets().inNamespace(namespace).withName("gitserver-config").delete();
        }
    }

    private void init() throws Exception {
        if (server == null) {
            server = getSpec();

            server = client.pods().inNamespace(namespace).withName(server.getMetadata().getName()).get();
            if (server == null) {
                server = client.pods().inNamespace(namespace).create(getSpec());
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
