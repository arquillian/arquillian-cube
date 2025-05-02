package org.arquillian.cube.openshift.impl.client;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroup;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.CatalogSource;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.Subscription;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.arquillian.cube.kubernetes.impl.utils.CommandExecutor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GitServer {
    private static final String ARQUILLIAN_GIT_ADMIN_USERNAME = "Arquillian";
    private static final String ARQUILLIAN_GIT_ADMIN_PASSWORD = "Cube";
    private static final String GITEA_INSTANCE_NAME = "gitea";
    public static final String GITEA_OPERATORGROUP_NAME = GITEA_INSTANCE_NAME + "-operator";
    public static final String GITEA_SUBSCRIPTION_NAME = GITEA_INSTANCE_NAME + "-operator";
    public static final String GITEA_NAMESPACE = GITEA_INSTANCE_NAME + "-operator";
    private final static String GITEA_CUSTOM_RESOURCE_RAW_JSON = "{" +
        "\"apiVersion\":\"pfe.rhpds.com/v1\"," +
        "\"kind\":\"Gitea\"," +
        "\"metadata\": {\"name\": \"" + GITEA_INSTANCE_NAME + "\"}," +
        "\"spec\": {" +
        "\"giteaImageTag\": \"1.20.0\"," +
        "\"giteaVolumeSize\": \"0.5Gi\"," +
        "\"postgresqlVolumeSize\": \"0.5Gi\"," +
        "\"giteaAdminUser\": \"" + ARQUILLIAN_GIT_ADMIN_USERNAME + "\"," +
        "\"giteaAdminEmail\": \"cube@discuss.arquillian.org\"," +
        "\"giteaAdminPassword\": \"" + ARQUILLIAN_GIT_ADMIN_PASSWORD + "\"" +
        "}}";
    public static final String GITEA_OPERATOR_DEPLOYMENT_URL = "https://github.com/rhpds/gitea-operator/OLMDeploy";

    private io.fabric8.openshift.client.OpenShiftClient client;
    private String namespace;
    private final CommandExecutor commandExecutor = new CommandExecutor();
    private boolean handleGiteaOperatorDeploymentLifecycle;
    private Namespace giteaNamespace;
    private Subscription giteaSubScription;
    private OperatorGroup giteaOperatorGroup;
    private CatalogSource giteaCatalogSource;
    private GenericKubernetesResource gitea;
    private String repoBaseUrl;
    private final ResourceDefinitionContext giteaResourceDefinitionContext = new ResourceDefinitionContext.Builder()
        .withGroup("pfe.rhpds.com")
        .withVersion("v1")
        .withPlural(GITEA_INSTANCE_NAME)
        .withNamespaced(true)
        .build();

    @Deprecated
    public GitServer(io.fabric8.openshift.client.OpenShiftClient client, Config config, String namespace) {
        this(client, namespace);
    }

    public GitServer(io.fabric8.openshift.client.OpenShiftClient client, String namespace) {
        this.client = client;
        this.namespace = namespace;
    }

    public URI push(File source, String name) throws Exception {
        // let's deploy and set up a Gitea service on OpenShift, if it hasn't been done already
        gitea = client.genericKubernetesResources(giteaResourceDefinitionContext)
            .inNamespace(namespace).withName(GITEA_INSTANCE_NAME).get();
        if (gitea == null) {
            init();
        }
        final String repoUrl = String.format("%s/" + ARQUILLIAN_GIT_ADMIN_USERNAME + "/%s", repoBaseUrl, name);

        // let's create a dedicated repo
        createRepo(name, repoBaseUrl);

        // Push via route
        File gitRoot = new File("target", name);
        FileUtils.copyDirectory(source, gitRoot);
        Git git = Git.init().setDirectory(gitRoot).call();
        Repository repo = git.getRepository();
        repo.getConfig().setString("remote", "origin", "url", repoUrl);
        repo.getConfig().save();

        git.add().addFilepattern(".").call();
        git.commit().setMessage("testing").setAuthor(ARQUILLIAN_GIT_ADMIN_USERNAME, "cube@discuss.arquillian.org").call();
        git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(ARQUILLIAN_GIT_ADMIN_USERNAME, ARQUILLIAN_GIT_ADMIN_PASSWORD)).setRemote("origin").setPushAll().setForce(true).call();
        repo.close();

        // Return an internal service name, for use within the openshift network
        final Service giteaService = client.services().inNamespace(namespace).withName(GITEA_INSTANCE_NAME).get();
        String serverUrl = String.format("http://%s:%s/" + ARQUILLIAN_GIT_ADMIN_USERNAME + "/%s",
            giteaService.getSpec().getClusterIP(),
            giteaService.getSpec().getPorts().get(0).getPort(), name);
        return URI.create(serverUrl);
    }

    private static void createRepo(final String repoName, final String gitRouteUrl) throws IOException, InterruptedException {
        final String repoCreationUrl = gitRouteUrl + "/api/v1/user/repos";
        final String repoCreationData = "{\n" +
            "  \"auto_init\": true,\n" +
            "  \"default_branch\": \"main\",\n" +
            "  \"description\": \"" + repoName + "\",\n" +
            "  \"name\": \"" + repoName + "\"\n" +
            "}";
        final HttpClient httpClient = HttpClient.newBuilder().build();
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(repoCreationUrl))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                (ARQUILLIAN_GIT_ADMIN_USERNAME + ":" + ARQUILLIAN_GIT_ADMIN_PASSWORD).getBytes(StandardCharsets.UTF_8)))
            .POST(HttpRequest.BodyPublishers.ofString(repoCreationData))
            .build();

        // it looks like Gitea needs a bit of time to be able and actually create the repo
        HttpResponse<?> response;
        int i = 1, attempts = 5;
        do {
            Thread.sleep(3_000);
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } while (response.statusCode() != HttpStatus.SC_CREATED  && i++ < attempts);
        // check response
        if (response.statusCode() != HttpStatus.SC_CREATED) {
            throw new IllegalStateException("Error while creating a Git repo: " + response.body().toString());
        }
    }

    public void shutdown() throws Exception {
        // delete the managed Gitea instance
        if (gitea != null) {
            client.genericKubernetesResources(giteaResourceDefinitionContext)
                .inNamespace(namespace).withName(GITEA_INSTANCE_NAME).delete();
        }
        // undeploy the Gitea operator if the lifecycle is handled here
        if (handleGiteaOperatorDeploymentLifecycle) {
            client.operatorHub().subscriptions().resource(giteaSubScription).delete();
            client.operatorHub().catalogSources().resource(giteaCatalogSource).delete();
            client.operatorHub().operatorGroups().resource(giteaOperatorGroup).delete();
            client.namespaces().resource(giteaNamespace).delete();
        }
    }

    private Deployment getGiteaOperatorControllerDeployment() {
        return client
            .apps().deployments()
            .inNamespace(GITEA_INSTANCE_NAME + "-operator")
            .withName(GITEA_INSTANCE_NAME + "-operator-controller-manager").get();
    }

    private void init() {
        handleGiteaOperatorDeploymentLifecycle = getGiteaOperatorControllerDeployment() == null;
        // Gitea operator installed _only_ if not there already.
        // It is installed in the gitea-operator namespace, and manages all the namespaces, so we don't want to duplicate
        if (handleGiteaOperatorDeploymentLifecycle) {
            commandExecutor.execCommand("oc apply -k " + GITEA_OPERATOR_DEPLOYMENT_URL);
            org.awaitility.Awaitility.await().atMost(2, TimeUnit.MINUTES).until(() -> {
                    return client
                        .apps().deployments()
                        .inNamespace(GITEA_NAMESPACE)
                        .withName(GITEA_INSTANCE_NAME + "-operator-controller-manager")
                        .isReady();
                }
            );
            giteaNamespace = client.namespaces().withName(GITEA_NAMESPACE).get();
            giteaSubScription = client.operatorHub().subscriptions().withName(GITEA_SUBSCRIPTION_NAME).get();
            giteaCatalogSource = client.operatorHub().catalogSources().withName("redhat-rhpds-" + GITEA_INSTANCE_NAME).get();
            giteaOperatorGroup = client.operatorHub().operatorGroups().withName(GITEA_OPERATORGROUP_NAME).get();
        }
        // let's create a managed Gitea instance
        gitea = client.genericKubernetesResources(giteaResourceDefinitionContext)
            .inNamespace(namespace).withName(GITEA_INSTANCE_NAME).get();
        if (gitea == null) {
            gitea = client.genericKubernetesResources(giteaResourceDefinitionContext)
                .inNamespace(namespace).load(IOUtils.toInputStream(GITEA_CUSTOM_RESOURCE_RAW_JSON)).create();

            org.awaitility.Awaitility.await().atMost(10, TimeUnit.MINUTES).until(() -> {
                    List<Pod> pods = client
                        .pods()
                        .inNamespace(namespace)
                        .withLabel("app", GITEA_INSTANCE_NAME).list().getItems();
                    return !pods.isEmpty() && pods.get(0).getStatus().getConditions().stream().anyMatch(c -> "Ready".equals(c.getType()) && "True".equals(c.getStatus()));
                }
            );
            repoBaseUrl = String.format("http://%s", client.routes().inNamespace(namespace).withName(GITEA_INSTANCE_NAME).get().getSpec().getHost());
        }
    }
}
