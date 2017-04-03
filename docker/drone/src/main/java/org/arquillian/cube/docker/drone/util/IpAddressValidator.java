package org.arquillian.cube.docker.drone.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IpAddressValidator {

    private static final String IPADDRESS_PATTERN =
        "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
    private static Pattern pattern;

    static {
        pattern = Pattern.compile(IPADDRESS_PATTERN);
    }

    /**
     * Validate ipv4 address with regular expression
     *
     * @param ip
     *     address for validation
     *
     * @return true valid ip address, false invalid ip address
     */
    public static boolean validate(final String ip) {
        Matcher matcher = pattern.matcher(ip);
        return matcher.matches();
    }
}
