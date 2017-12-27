package org.arquillian.cube.impl.client;

import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.docker.impl.requirement.RequiresDockerMachine;
import org.arquillian.cube.spi.BaseCube;
import org.arquillian.cube.spi.Binding;
import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeControlException;
import org.arquillian.cube.spi.metadata.CubeMetadata;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({RequiresDocker.class, RequiresDockerMachine.class})
public class CubeMetadataTest {

    @Test
    public void shouldReportCorrectStateOfMetadata() {
        Cube<?> cube = new TestCube();
        cube.addMetadata(TestMetadata.class, new TestMetadataImpl());

        Assert.assertTrue(cube.hasMetadata(TestMetadata.class));
    }

    @Test
    public void shouldGetCorrectMetadata() {
        Cube<?> cube = new TestCube();
        cube.addMetadata(TestMetadata.class, new TestMetadataImpl());

        TestMetadata metadata = cube.getMetadata(TestMetadata.class);

        Assert.assertTrue(metadata instanceof TestMetadataImpl);
    }

    private static interface TestMetadata extends CubeMetadata {
        String get();
    }

    private static class TestMetadataImpl implements TestMetadata {

        @Override
        public String get() {
            return "A";
        }
    }

    private static class TestCube extends BaseCube<Void> {

        @Override
        public org.arquillian.cube.spi.Cube.State state() {
            return null;
        }

        @Override
        public String getId() {
            return null;
        }

        @Override
        public void create() throws CubeControlException {
        }

        @Override
        public void start() throws CubeControlException {
        }

        @Override
        public void stop() throws CubeControlException {
        }

        @Override
        public void destroy() throws CubeControlException {
        }

        @Override
        public boolean isRunningOnRemote() {
            return false;
        }

        @Override
        public void changeToPreRunning() {
        }

        @Override
        public Binding bindings() {
            return null;
        }

        @Override
        public Binding configuredBindings() {
            return null;
        }

        @Override
        public Void configuration() {
            return null;
        }
    }
}
