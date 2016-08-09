package org.arquillian.cube.impl.client.enricher;


import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.arquillian.cube.HostIp;
import org.arquillian.cube.HostIpContext;
import org.jboss.arquillian.core.api.Instance;
import org.junit.Test;

public class HostIpTestEnricherTest {


    @Test
    public void shouldEnrichTest() {
        HostIpTestEnricher hostIpTestEnricher = new HostIpTestEnricher();
        hostIpTestEnricher.hostUriContext = new Instance<HostIpContext>() {
            @Override
            public HostIpContext get() {
                return new HostIpContext("192.168.99.100");
            }
        };
        MyTest test = new MyTest();
        hostIpTestEnricher.enrich(test);
        assertThat(test.hostIp, is("192.168.99.100"));
    }

    @Test
    public void shouldEnrichTestMethod() throws NoSuchMethodException {
        HostIpTestEnricher hostIpTestEnricher = new HostIpTestEnricher();
        hostIpTestEnricher.hostUriContext = new Instance<HostIpContext>() {
            @Override
            public HostIpContext get() {
                return new HostIpContext("192.168.99.100");
            }
        };
        MyTest test = new MyTest();
        Object[] myMethods = hostIpTestEnricher.resolve(test.getClass().getMethod("myMethod", String.class, String.class));
        assertThat((String)myMethods[1], is("192.168.99.100"));
    }

    public static class MyTest {
        @HostIp
        String hostIp;

        public void myMethod(String first, @HostIp String hostIp) {

        }
    }

}
