package org.arquillian.cube.openshift.impl.resources;

import org.arquillian.cube.impl.util.IOUtil;
import org.arquillian.cube.openshift.api.OpenShiftDynamicImageStreamResource;
import org.arquillian.cube.openshift.impl.adapter.OpenShiftAdapter;
import org.arquillian.cube.openshift.impl.client.CubeOpenShiftConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@OpenShiftDynamicImageStreamResource(name = "stream-1.0", image = "registry.host.com/imageFamily/imageName", version = "1.0", insecure = "true")
class OpenShiftDynamicImageStreamResourceExample {

}

@OpenShiftDynamicImageStreamResource(name = "stream-1.0", image = "registry.host.com/imageFamily/imageName", version = "1.0", insecure = "false")
class SecureOpenShiftDynamicImageStreamResourceExample {

}

@OpenShiftDynamicImageStreamResource(name = "stream-1.0", image = "registry.host.com/imageFamily/one", version = "1.0", insecure = "false")
@OpenShiftDynamicImageStreamResource(name = "stream-2.0", image = "registry.host.com/imageFamily/two", version = "2.0", insecure = "true")
class MultipleOpenShiftDynamicImageStreamResourceExample {

}

@RunWith(MockitoJUnitRunner.class)
public class OpenShiftResourceFactoryTest {

    @Mock
    OpenShiftAdapter adapter;

    @Mock
    CubeOpenShiftConfiguration configuration;

    @Before
    public void prepareEnv() {
        when(configuration.getProperties()).thenReturn(new Properties());
    }

    @Test
    public void testInsecureDynamicOpenShiftResource() throws Exception {
        OpenShiftResourceFactory.createResources("key", adapter, OpenShiftDynamicImageStreamResourceExample.class, configuration.getProperties());
        ArgumentCaptor<ByteArrayInputStream> captor = ArgumentCaptor.forClass(ByteArrayInputStream.class);

        verify(adapter, times(1)).createResource(eq("key"), captor.capture());
        assertEquals("{\"metadata\":{\"name\":\"stream-1.0\",\"annotations\":{\"openshift.io\\/image.insecureRepository\":true}},\"apiVersion\":\"v1\",\"kind\":\"ImageStream\",\"spec\":{\"tags\":[{\"importPolicy\":{\"insecure\":true},\"name\":\"1.0\",\"annotations\":{\"version\":\"1.0\"},\"from\":{\"kind\":\"DockerImage\",\"name\":\"registry.host.com\\/imageFamily\\/imageName\"}}]}}", IOUtil.asString(captor.getValue()));
    }

    @Test
    public void testSecureDynamicOpenShiftResource() throws Exception {
        OpenShiftResourceFactory.createResources("key", adapter, SecureOpenShiftDynamicImageStreamResourceExample.class, configuration.getProperties());
        ArgumentCaptor<ByteArrayInputStream> captor = ArgumentCaptor.forClass(ByteArrayInputStream.class);

        verify(adapter, times(1)).createResource(eq("key"), captor.capture());
        assertEquals("{\"metadata\":{\"name\":\"stream-1.0\",\"annotations\":{\"openshift.io\\/image.insecureRepository\":false}},\"apiVersion\":\"v1\",\"kind\":\"ImageStream\",\"spec\":{\"tags\":[{\"importPolicy\":{\"insecure\":false},\"name\":\"1.0\",\"annotations\":{\"version\":\"1.0\"},\"from\":{\"kind\":\"DockerImage\",\"name\":\"registry.host.com\\/imageFamily\\/imageName\"}}]}}", IOUtil.asString(captor.getValue()));
    }

    @Test
    public void testMultipleDynamicOpenShiftResource() throws Exception {
        OpenShiftResourceFactory.createResources("key", adapter, MultipleOpenShiftDynamicImageStreamResourceExample.class, configuration.getProperties());
        ArgumentCaptor<ByteArrayInputStream> captor = ArgumentCaptor.forClass(ByteArrayInputStream.class);

        verify(adapter, times(2)).createResource(eq("key"), captor.capture());

        List<ByteArrayInputStream> is = captor.getAllValues();

        assertEquals(2, is.size());
        assertEquals("{\"metadata\":{\"name\":\"stream-1.0\",\"annotations\":{\"openshift.io\\/image.insecureRepository\":false}},\"apiVersion\":\"v1\",\"kind\":\"ImageStream\",\"spec\":{\"tags\":[{\"importPolicy\":{\"insecure\":false},\"name\":\"1.0\",\"annotations\":{\"version\":\"1.0\"},\"from\":{\"kind\":\"DockerImage\",\"name\":\"registry.host.com\\/imageFamily\\/one\"}}]}}", IOUtil.asString(is.get(0)));
        assertEquals("{\"metadata\":{\"name\":\"stream-2.0\",\"annotations\":{\"openshift.io\\/image.insecureRepository\":true}},\"apiVersion\":\"v1\",\"kind\":\"ImageStream\",\"spec\":{\"tags\":[{\"importPolicy\":{\"insecure\":true},\"name\":\"2.0\",\"annotations\":{\"version\":\"2.0\"},\"from\":{\"kind\":\"DockerImage\",\"name\":\"registry.host.com\\/imageFamily\\/two\"}}]}}", IOUtil.asString(is.get(1)));
    }
}
