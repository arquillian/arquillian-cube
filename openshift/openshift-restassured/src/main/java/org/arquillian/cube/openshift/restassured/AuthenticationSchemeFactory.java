package org.arquillian.cube.openshift.restassured;

import io.restassured.RestAssured;
import io.restassured.authentication.AuthenticationScheme;

import java.util.Arrays;

public class AuthenticationSchemeFactory {

    public static AuthenticationScheme create(String attribute) {
        String[] parameters = attribute.split(":");

        if (parameters.length < 2) {
            throw new IllegalArgumentException(
                String.format("Authentication scheme %s doesn't follow the standard format <protocol>:(<value>[:])+"));
        }

        switch (parameters[0]) {
            case "basic": {
                validateEntry(parameters, 3);
                return RestAssured.basic(parameters[1], parameters[2]);
            }
            case "form": {
                validateEntry(parameters, 3);
                return RestAssured.form(parameters[1], parameters[2]);
            }
            case "preemptive": {
                validateEntry(parameters, 3);
                return RestAssured.preemptive().basic(parameters[1], parameters[2]);
            }
            case "certificate": {
                validateEntryBigger(parameters, 3);
                final String[] url = Arrays.copyOfRange(parameters, 2, parameters.length - 1);
                return RestAssured.certificate(join(url), parameters[parameters.length - 1]);
            }
            case "digest": {
                validateEntry(parameters, 3);
                return RestAssured.digest(parameters[1], parameters[2]);
            }
            case "oauth": {
                validateEntry(parameters, 5);
                return RestAssured.oauth(parameters[1], parameters[2], parameters[3], parameters[4]);
            }
            case "oauth2": {
                validateEntry(parameters, 2);
                return RestAssured.oauth2(parameters[1]);
            }
            default:
                throw new IllegalArgumentException(String.format("Unrecognized protocol %s", parameters[0]));
        }
    }

    private static String join(String[] elements) {
        StringBuilder builder = new StringBuilder();
        for (String s : elements) {
            builder.append(s);
        }
        return builder.toString();
    }

    private static void validateEntryBigger(String[] parameters, int numberOfValid) {
        if (parameters.length < numberOfValid) {
            throw new IllegalArgumentException(
                String.format("Invalid number of parameters for %s command.", Arrays.toString(parameters)));
        }
    }

    private static void validateEntry(String[] parameters, int numberOfValid) {
        if (parameters.length != numberOfValid) {
            throw new IllegalArgumentException(
                String.format("Invalid number of parameters for %s command.", Arrays.toString(parameters)));
        }
    }
}
