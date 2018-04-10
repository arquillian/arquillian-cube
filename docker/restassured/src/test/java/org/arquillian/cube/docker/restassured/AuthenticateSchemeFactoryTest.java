package org.arquillian.cube.docker.restassured;

import io.restassured.authentication.AuthenticationScheme;
import io.restassured.authentication.BasicAuthScheme;
import io.restassured.authentication.CertAuthScheme;
import io.restassured.authentication.FormAuthScheme;
import io.restassured.authentication.OAuthScheme;
import io.restassured.authentication.PreemptiveBasicAuthScheme;
import io.restassured.authentication.PreemptiveOAuth2HeaderScheme;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthenticateSchemeFactoryTest {

    @Test
    public void should_create_basic_auth() {
        final AuthenticationScheme authenticationScheme = AuthenticationSchemeFactory.create("basic:username:password");
        assertThat(authenticationScheme).isInstanceOf(BasicAuthScheme.class);
        BasicAuthScheme basicAuthScheme = (BasicAuthScheme) authenticationScheme;
        assertThat(basicAuthScheme.getUserName()).isEqualTo("username");
        assertThat(basicAuthScheme.getPassword()).isEqualTo("password");
    }

    @Test
    public void should_create_form_auth() {
        final AuthenticationScheme authenticationScheme = AuthenticationSchemeFactory.create("form:username:password");
        assertThat(authenticationScheme).isInstanceOf(FormAuthScheme.class);
        FormAuthScheme authScheme = (FormAuthScheme) authenticationScheme;
        assertThat(authScheme.getUserName()).isEqualTo("username");
        assertThat(authScheme.getPassword()).isEqualTo("password");
    }

    @Test
    public void should_create_preemptive_auth() {
        final AuthenticationScheme authenticationScheme =
            AuthenticationSchemeFactory.create("preemptive:username:password");
        assertThat(authenticationScheme).isInstanceOf(PreemptiveBasicAuthScheme.class);
        PreemptiveBasicAuthScheme authScheme = (PreemptiveBasicAuthScheme) authenticationScheme;
        assertThat(authScheme.getUserName()).isEqualTo("username");
        assertThat(authScheme.getPassword()).isEqualTo("password");
    }

    @Test
    public void should_create_certificate_auth() {
        final AuthenticationScheme authenticationScheme =
            AuthenticationSchemeFactory.create("certificate:file:///url:password");
        assertThat(authenticationScheme).isInstanceOf(CertAuthScheme.class);
        CertAuthScheme authScheme = (CertAuthScheme) authenticationScheme;

        assertThat(authScheme.getTrustStorePassword()).isEqualTo("password");
        assertThat(authScheme.getPathToTrustStore()).isEqualTo("///url");
    }

    @Test
    public void should_create_digest_auth() {
        final AuthenticationScheme authenticationScheme = AuthenticationSchemeFactory.create("digest:username:password");
        assertThat(authenticationScheme).isInstanceOf(BasicAuthScheme.class);
        BasicAuthScheme authScheme = (BasicAuthScheme) authenticationScheme;
        assertThat(authScheme.getUserName()).isEqualTo("username");
        assertThat(authScheme.getPassword()).isEqualTo("password");
    }

    @Test
    public void should_create_oauth_auth() {
        final AuthenticationScheme authenticationScheme =
            AuthenticationSchemeFactory.create("oauth:consumerKey:consumerSecret:accessToken:secretToken");
        assertThat(authenticationScheme).isInstanceOf(OAuthScheme.class);
        OAuthScheme authScheme = (OAuthScheme) authenticationScheme;

        assertThat(authScheme.getAccessToken()).isEqualTo("accessToken");
        assertThat(authScheme.getConsumerKey()).isEqualTo("consumerKey");
        assertThat(authScheme.getConsumerSecret()).isEqualTo("consumerSecret");
        assertThat(authScheme.getSecretToken()).isEqualTo("secretToken");
    }

    @Test
    public void should_create_oauth_auth2() {
        final AuthenticationScheme authenticationScheme = AuthenticationSchemeFactory.create("oauth2:accessToken");
        assertThat(authenticationScheme).isInstanceOf(PreemptiveOAuth2HeaderScheme.class);
        PreemptiveOAuth2HeaderScheme authScheme = (PreemptiveOAuth2HeaderScheme) authenticationScheme;

        assertThat(authScheme.getAccessToken()).isEqualTo("accessToken");
    }
}
