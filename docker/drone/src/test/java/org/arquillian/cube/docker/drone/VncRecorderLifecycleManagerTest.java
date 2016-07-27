package org.arquillian.cube.docker.drone;

import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.arquillian.test.spi.TestResult;
import org.jboss.arquillian.test.spi.event.suite.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VncRecorderLifecycleManagerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    CubeRegistry cubeRegistry;

    @Mock
    Cube cube;

    @Mock
    After after;

    @Mock
    SeleniumContainers seleniumContainers;

    @Test
    public void shouldStartVncBeforeEachTestByDefault() {

        when(cubeRegistry.getCube(SeleniumContainers.VNC_CONTAINER_NAME)).thenReturn(cube);

        VncRecorderLifecycleManager vncRecorderLifecycleManager = new VncRecorderLifecycleManager();
        vncRecorderLifecycleManager.startRecording(null,
                CubeDroneConfiguration.fromMap(new HashMap<String, String>()),
                cubeRegistry);

        verify(cube).create();
        verify(cube).start();

    }

    @Test
    public void shouldMoveRecordingVideo() throws IOException, NoSuchMethodException {

        final File destination = temporaryFolder.newFolder("destination");
        final File video = temporaryFolder.newFile("file.flv");

        when(seleniumContainers.getVideoRecordingFile()).thenReturn(video.toPath());
        when(after.getTestClass()).thenReturn(new TestClass(VncRecorderLifecycleManagerTest.class));
        when(after.getTestMethod()).thenReturn(VncRecorderLifecycleManagerTest.class.getMethod("shouldMoveRecordingVideo"));

        Map<String, String> conf = new HashMap<>();
        conf.put("videoOutput", destination.getAbsolutePath());

        TestResult testResult = TestResult.passed();

        VncRecorderLifecycleManager vncRecorderLifecycleManager = new VncRecorderLifecycleManager();
        vncRecorderLifecycleManager.vnc = cube;
        vncRecorderLifecycleManager.stopRecording(after,
                testResult,
                CubeDroneConfiguration.fromMap(conf),
                seleniumContainers
                );

        assertThat(new File(destination, "org_arquillian_cube_docker_drone_VncRecorderLifecycleManagerTest_shouldMoveRecordingVideo.flv").exists(), is(true));
    }

    @Test
    public void shouldStopVncAfterEachTestByDefault() throws IOException, NoSuchMethodException {

        final File destination = temporaryFolder.newFolder("destination");
        final File video = temporaryFolder.newFile("file.flv");

        when(seleniumContainers.getVideoRecordingFile()).thenReturn(video.toPath());
        when(after.getTestClass()).thenReturn(new TestClass(VncRecorderLifecycleManagerTest.class));
        when(after.getTestMethod()).thenReturn(VncRecorderLifecycleManagerTest.class.getMethod("shouldMoveRecordingVideo"));

        Map<String, String> conf = new HashMap<>();
        conf.put("videoOutput", destination.getAbsolutePath());

        TestResult testResult = TestResult.passed();

        VncRecorderLifecycleManager vncRecorderLifecycleManager = new VncRecorderLifecycleManager();
        vncRecorderLifecycleManager.vnc = cube;
        vncRecorderLifecycleManager.stopRecording(after,
                testResult,
                CubeDroneConfiguration.fromMap(conf),
                seleniumContainers
        );

        verify(cube).stop();
        verify(cube).destroy();
    }

    @Test
    public void shouldDiscardRecordingIfConfiguredInOnlyFailingAndPassedTest() throws IOException, NoSuchMethodException {

        final File destination = temporaryFolder.newFolder("destination");
        final File video = temporaryFolder.newFile("file.flv");

        when(seleniumContainers.getVideoRecordingFile()).thenReturn(video.toPath());
        when(after.getTestClass()).thenReturn(new TestClass(VncRecorderLifecycleManagerTest.class));
        when(after.getTestMethod()).thenReturn(VncRecorderLifecycleManagerTest.class.getMethod("shouldDiscardRecordingIfConfiguredInOnlyFailingAndPassedTest"));

        Map<String, String> conf = new HashMap<>();
        conf.put("videoOutput", destination.getAbsolutePath());
        conf.put("recordingMode", "ONLY_FAILING");

        TestResult testResult = TestResult.passed();

        VncRecorderLifecycleManager vncRecorderLifecycleManager = new VncRecorderLifecycleManager();
        vncRecorderLifecycleManager.vnc = cube;
        vncRecorderLifecycleManager.stopRecording(after,
                testResult,
                CubeDroneConfiguration.fromMap(conf),
                seleniumContainers
        );

        assertThat(new File(destination, "org_arquillian_cube_docker_drone_VncRecorderLifecycleManagerTest_shouldDiscardRecordingIfConfiguredInOnlyFailingAndPassedTest.flv").exists(), is(false));
    }

    @Test
    public void shouldMoveRecordingIfConfiguredInOnlyFailingAndFailedTest() throws IOException, NoSuchMethodException {

        final File destination = temporaryFolder.newFolder("destination");
        final File video = temporaryFolder.newFile("file.flv");

        when(seleniumContainers.getVideoRecordingFile()).thenReturn(video.toPath());
        when(after.getTestClass()).thenReturn(new TestClass(VncRecorderLifecycleManagerTest.class));
        when(after.getTestMethod()).thenReturn(VncRecorderLifecycleManagerTest.class.getMethod("shouldDiscardRecordingIfConfiguredInOnlyFailingAndPassedTest"));

        Map<String, String> conf = new HashMap<>();
        conf.put("videoOutput", destination.getAbsolutePath());
        conf.put("recordingMode", "ONLY_FAILING");

        TestResult testResult = TestResult.failed(new Throwable());

        VncRecorderLifecycleManager vncRecorderLifecycleManager = new VncRecorderLifecycleManager();
        vncRecorderLifecycleManager.vnc = cube;
        vncRecorderLifecycleManager.stopRecording(after,
                testResult,
                CubeDroneConfiguration.fromMap(conf),
                seleniumContainers
        );

        assertThat(new File(destination, "org_arquillian_cube_docker_drone_VncRecorderLifecycleManagerTest_shouldDiscardRecordingIfConfiguredInOnlyFailingAndPassedTest.flv").exists(), is(true));
    }

}
