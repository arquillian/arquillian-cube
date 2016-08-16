package org.arquillian.cube.docker.restassured;

import io.restassured.authentication.AuthenticationScheme;

import java.util.Map;

public class RestAssuredConfiguration {

    private static final String RELAXED_HTTPS_VALIDATION_ALL_PROTOCOLS = "";

    private String baseUri;
    private String schema = "http";
    private int port = -1;
    private String basePath;
    private String rootPath;
    private AuthenticationScheme authenticationScheme;
    private String useRelaxedHttpsValidation;
    private String[] exclusionContainers = new String[0];

    public String getUseRelaxedHttpsValidation() {
        return useRelaxedHttpsValidation;
    }

    public boolean isUseRelaxedHttpsValidationSet() {
        return this.useRelaxedHttpsValidation != null;
    }

    public boolean isUseRelaxedHttpsValidationInAllProtocols() {
        return RELAXED_HTTPS_VALIDATION_ALL_PROTOCOLS.equals(this.useRelaxedHttpsValidation);
    }

    public boolean isAuthenticationSchemeSet() {
        return this.authenticationScheme != null;
    }

    public AuthenticationScheme getAuthenticationScheme() {
        return authenticationScheme;
    }

    public boolean isPortSet() {
        return port > -1;
    }

    public int getPort() {
        return port;
    }

    public boolean isBasePathSet() {
        return basePath != null && !basePath.isEmpty();
    }

    public String getBasePath() {
        return basePath;
    }

    public boolean isBaseUriSet() {
        return baseUri != null && !baseUri.isEmpty();
    }

    public String getBaseUri() {
        return baseUri;
    }

    public boolean isRootPath() {
        return rootPath != null && !rootPath.isEmpty();
    }

    public String getRootPath() {
        return rootPath;
    }

    public String[] getExclusionContainers() {
        return exclusionContainers;
    }

    public String getSchema() {
        return schema;
    }

    public static RestAssuredConfiguration fromMap(final Map<String, String> conf) {
        RestAssuredConfiguration restAssuredConfiguration = new RestAssuredConfiguration();

        if (conf.containsKey("baseUri")) {
            restAssuredConfiguration.baseUri = conf.get("baseUri");
        }

        if (conf.containsKey("port")) {
            restAssuredConfiguration.port= Integer.parseInt(conf.get("port"));
        }

        if (conf.containsKey("basePath")) {
            restAssuredConfiguration.basePath = conf.get("basePath");
        }

        if (conf.containsKey("rootPath")) {
            restAssuredConfiguration.rootPath = conf.get("rootPath");
        }

        if (conf.containsKey("authenticationScheme")) {
            restAssuredConfiguration.authenticationScheme = AuthenticationSchemeFactory.create(conf.get("authenticationScheme"));
        }

        if (conf.containsKey("schema")) {
            restAssuredConfiguration.schema = conf.get("schema");
        }

        if (conf.containsKey("exclusionContainers")) {
            restAssuredConfiguration.exclusionContainers = conf.get("exclusionContainers").split(",");
            for(int i=0; i <restAssuredConfiguration.exclusionContainers.length; i++) {
                restAssuredConfiguration.exclusionContainers[i] = restAssuredConfiguration.exclusionContainers[i].trim();
            }
        }

        if( conf.containsKey("useRelaxedHttpsValidation")) {
            final String useRelaxedHttpsValidation = conf.get("useRelaxedHttpsValidation");

            if (useRelaxedHttpsValidation == null || useRelaxedHttpsValidation.isEmpty()) {
                restAssuredConfiguration.useRelaxedHttpsValidation = RELAXED_HTTPS_VALIDATION_ALL_PROTOCOLS;
            } else {
                restAssuredConfiguration.useRelaxedHttpsValidation = useRelaxedHttpsValidation;
            }

        }

        return restAssuredConfiguration;
    }
}
