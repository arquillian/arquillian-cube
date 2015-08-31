package org.arquillian.cube;

import java.net.URI;

public class HostUriContext {

    private URI hostUri;

    public HostUriContext(String hostUri) {
        this.hostUri = URI.create(hostUri);
    }

    public String getHost() {
        return this.hostUri.getHost();
    }
}
