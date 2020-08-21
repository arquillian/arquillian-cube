package org.arquillian.cube.kubernetes.impl;

import io.fabric8.kubernetes.api.builder.v4_10.TypedVisitor;
import io.fabric8.kubernetes.clnt.v4_10.ConfigBuilder;
import org.arquillian.cube.impl.util.Strings;
import org.arquillian.cube.kubernetes.api.Configuration;

public class ClientConfigBuilder {

    public ConfigBuilder configuration(Configuration config) {
        final ConfigBuilder configBuilder = new ConfigBuilder()
            .withNamespace(config.getNamespace())
            .withApiVersion(config.getApiVersion())
            .withTrustCerts(config.isTrustCerts())
            .accept(new TypedVisitor<ConfigBuilder>() {
                @Override
                public void visit(ConfigBuilder b) {
                    b.withNoProxy(b.getNoProxy() == null ? new String[0] : b.getNoProxy());
                }
            });

        if (Strings.isNotNullOrEmpty(config.getMasterUrl().toString())) {
            configBuilder.withMasterUrl(config.getMasterUrl().toString());
        }

        if (Strings.isNotNullOrEmpty(config.getToken())) {
            configBuilder.withOauthToken(config.getToken());
        }

        if (Strings.isNotNullOrEmpty(config.getUsername()) && Strings.isNotNullOrEmpty(config.getPassword())) {
            configBuilder.withUsername(config.getUsername())
                .withPassword(config.getPassword());
        }
        return configBuilder;
    }
}
