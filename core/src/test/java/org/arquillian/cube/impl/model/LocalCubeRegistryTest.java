package org.arquillian.cube.impl.model;

import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

public class LocalCubeRegistryTest {

    private CubeRegistry cubeRegistry;

    @Before
    public void setUp() throws Exception {
        cubeRegistry = new LocalCubeRegistry();
    }

    @Test
    public void shouldAddAndRemoveCube() throws Exception {
        // given:
        String cubeId = "tomcat";
        Cube cube = createCubeMock(cubeId);

        // when:
        cubeRegistry.addCube(cube);

        // then:
        Cube<?> resolvedCube = cubeRegistry.getCube(cubeId);
        Assert.assertSame(cube, resolvedCube);

        // when:
        cubeRegistry.removeCube(cubeId);

        // then:
        Cube<?> resolvedCubeAfterRemove = cubeRegistry.getCube(cubeId);
        Assert.assertNull(resolvedCubeAfterRemove);
    }

    private Cube createCubeMock(String cubeId) {
        Cube cube = Mockito.mock(Cube.class);
        Mockito.doReturn(cubeId).when(cube).getId();

        return cube;
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotBeAbleToAddCubeByStarredCubeId() throws Exception {
        // given:
        String cubeId = "tomcat*";
        Cube cube = createCubeMock(cubeId);

        // when:
        cubeRegistry.addCube(cube);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotBeAbleToRemoveCubeByStarredCubeId() throws Exception {
        // given:
        String cubeId = "tomcat*";

        // when:
        cubeRegistry.removeCube(cubeId);
    }

    @Test
    public void shouldGetAllCubes() throws Exception {
        // given:
        List<String> cubeIds = Arrays.asList("tomcat1", "tomcat2", "tomcat_46fd2cc1-0084-42a8-9ffd-35f305a08dcc");

        for (String cubeId : cubeIds) {
            Cube cube = createCubeMock(cubeId);
            cubeRegistry.addCube(cube);
        }

        // when:
        List<Cube<?>> cubes = cubeRegistry.getCubes();

        // then:
        Assert.assertEquals(cubeIds.size(), cubes.size());
    }

    @Test
    public void shouldGetCubeByStarredCubeId() throws Exception {
        // given:
        String cubeId = "tomcat_46fd2cc1-0084-42a8-9ffd-35f305a08dcc";
        Cube cube = createCubeMock(cubeId);
        cubeRegistry.addCube(cube);

        // when:
        Cube<?> resolvedCube = cubeRegistry.getCube("tomcat*");

        // then:
        Assert.assertSame(cube, resolvedCube);
    }

}