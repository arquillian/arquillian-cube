package org.arquillian.cube.openshift.impl.oauth;

import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.http.HttpRequest;
import io.fabric8.kubernetes.client.http.HttpResponse;
import io.fabric8.kubernetes.client.jdkhttp.JdkHttpClientFactory;

import java.net.URL;
import java.util.Base64;
import java.util.logging.Logger;

/**
 * Created by fspolti on 6/20/16.
 */
public class OauthUtils {
    private static final Logger log = Logger.getLogger(OauthUtils.class.getName());

    private static final String TOKEN_REQUEST_URI = "oauth/token/request?client_id=openshift-challenging-client";
    private static final String OPENSHIFT_URL = "https://localhost:8443";
    private static final String USERNAME = "guest";
    private static final String PASSWORD = "guest";

    public static String getToken(String openshiftUrl, String uid, String pwd) throws Exception {
        String url = openshiftUrl != null ? openshiftUrl : OPENSHIFT_URL;
        String username = uid != null ? uid : USERNAME;
        String password = pwd != null ? pwd : PASSWORD;

        log.info("Issuing a new token for user: " + username);
        final HttpClient.Builder httpClientBuilder = new JdkHttpClientFactory().newBuilder();
        final HttpClient httpClient = httpClientBuilder.build();
        final HttpRequest request = httpClient.newHttpRequestBuilder()
            .url(new URL(url + "/" + TOKEN_REQUEST_URI))
            .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes()))
            .build();

        HttpResponse<String> response = httpClient.sendAsync(request, String.class).get();

        String result = response.body();

        String token = result.substring(result.indexOf("<code>") + 6, result.indexOf("</code>"));
        log.info("Got token: " + token);

        return token;
    }

}
