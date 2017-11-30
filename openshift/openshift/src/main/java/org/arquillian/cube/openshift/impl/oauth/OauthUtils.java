package org.arquillian.cube.openshift.impl.oauth;

import java.util.Base64;
import java.util.logging.Logger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
            .url(url + "/" + TOKEN_REQUEST_URI)
            .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes()))
            .build();

        Response response = client.newCall(request).execute();

        String result = response.body().string();

        String token = result.substring(result.indexOf("<code>") + 6, result.indexOf("</code>"));
        log.info("Got token: " + token);

        return token;
    }

}
