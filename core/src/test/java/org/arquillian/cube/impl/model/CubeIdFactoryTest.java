package org.arquillian.cube.impl.model;

import org.junit.Assert;
import org.junit.Test;

public class CubeIdFactoryTest {

    @Test(expected = NullPointerException.class)
    public void shouldThrowNPEOnNull() throws Exception {
        CubeIdFactory.get().create(null);
    }

    @Test
    public void shouldCreateStarredCubeId() throws Exception {
        CubeId cubeId = CubeIdFactory.get().create("tomcat*");

        Assert.assertTrue(cubeId instanceof StarredCubeId);
    }

    @Test
    public void shouldCreateParallelizedCubeId() throws Exception {
        CubeId cubeId = CubeIdFactory.get().create("tomcat_46fd2cc1-0084-42a8-9ffd-35f305a08dcc");

        Assert.assertTrue(cubeId instanceof ParallelizedCubeId);
    }

    @Test
    public void shouldCreateDefaultCubeId() throws Exception {
        CubeId cubeId = CubeIdFactory.get().create("tomcat");

        Assert.assertTrue(cubeId instanceof DefaultCubeId);
    }
}