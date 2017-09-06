package org.arquillian.cube.docker.drone;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.arquillian.cube.spi.Cube;
import org.arquillian.cube.spi.CubeRegistry;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.arquillian.test.spi.TestResult;
import org.jboss.arquillian.test.spi.event.suite.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
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
    Event event;

    @Mock
    SeleniumContainers seleniumContainers;

    @Test
    public void should_start_vnc_by_default() {
        when(seleniumContainers.getVncContainerName()).thenReturn("vnc");
        when(cubeRegistry.getCube("vnc")).thenReturn(cube);

        VncRecorderLifecycleManager vncRecorderLifecycleManager = new VncRecorderLifecycleManager();

        vncRecorderLifecycleManager.seleniumContainersInstance = new Instance<SeleniumContainers>() {
            @Override
            public SeleniumContainers get() {
                return seleniumContainers;
            }
        };
        
        vncRecorderLifecycleManager.startRecording(null,
            CubeDroneConfiguration.fromMap(new HashMap<String, String>()),
            cubeRegistry);

        verify(cube).create();
        verify(cube).start();
    }

    @Test
    public void should_move_recording_video() throws IOException, NoSuchMethodException {

        final File destination = temporaryFolder.newFolder("destination");
        final File video = temporaryFolder.newFile("file.flv");
        Files.write(video.toPath(), "Hello".getBytes());

        when(seleniumContainers.getVideoRecordingFile()).thenReturn(video.toPath());
        when(after.getTestClass()).thenReturn(new TestClass(VncRecorderLifecycleManagerTest.class));
        when(after.getTestMethod()).thenReturn(
            VncRecorderLifecycleManagerTest.class.getMethod("should_move_recording_video"));

        Map<String, String> conf = new HashMap<>();
        conf.put("videoOutput", destination.getAbsolutePath());

        TestResult testResult = TestResult.passed();

        VncRecorderLifecycleManager vncRecorderLifecycleManager = new VncRecorderLifecycleManager();
        vncRecorderLifecycleManager.vnc = cube;
        vncRecorderLifecycleManager.afterVideoRecordedEvent = event;
        vncRecorderLifecycleManager.stopRecording(after,
            testResult,
            CubeDroneConfiguration.fromMap(conf),
            seleniumContainers
        );

        assertThat(new File(destination,
            "org_arquillian_cube_docker_drone_VncRecorderLifecycleManagerTest_should_move_recording_video.flv"))
            .exists()
            .hasContent("Hello");
    }

    @Test
    public void should_stop_vnc_by_default() throws IOException, NoSuchMethodException {

        final File destination = temporaryFolder.newFolder("destination");
        final File video = temporaryFolder.newFile("file.flv");

        when(seleniumContainers.getVideoRecordingFile()).thenReturn(video.toPath());
        when(after.getTestClass()).thenReturn(new TestClass(VncRecorderLifecycleManagerTest.class));
        when(after.getTestMethod()).thenReturn(
            VncRecorderLifecycleManagerTest.class.getMethod("should_stop_vnc_by_default"));

        Map<String, String> conf = new HashMap<>();
        conf.put("videoOutput", destination.getAbsolutePath());

        TestResult testResult = TestResult.passed();

        VncRecorderLifecycleManager vncRecorderLifecycleManager = new VncRecorderLifecycleManager();
        vncRecorderLifecycleManager.vnc = cube;
        vncRecorderLifecycleManager.afterVideoRecordedEvent = event;
        vncRecorderLifecycleManager.stopRecording(after,
            testResult,
            CubeDroneConfiguration.fromMap(conf),
            seleniumContainers
        );

        verify(cube).stop();
        verify(cube).destroy();
    }

    @Test
    public void should_discard_recording_if_configured_in_only_failing_and_passed_test()
        throws IOException, NoSuchMethodException {

        final File destination = temporaryFolder.newFolder("destination");
        final File video = temporaryFolder.newFile("file.flv");

        when(seleniumContainers.getVideoRecordingFile()).thenReturn(video.toPath());
        when(after.getTestClass()).thenReturn(new TestClass(VncRecorderLifecycleManagerTest.class));
        when(after.getTestMethod()).thenReturn(VncRecorderLifecycleManagerTest.class.getMethod(
            "should_discard_recording_if_configured_in_only_failing_and_passed_test"));

        Map<String, String> conf = new HashMap<>();
        conf.put("videoOutput", destination.getAbsolutePath());
        conf.put("recordingMode", "ONLY_FAILING");

        TestResult testResult = TestResult.passed();

        VncRecorderLifecycleManager vncRecorderLifecycleManager = new VncRecorderLifecycleManager();
        vncRecorderLifecycleManager.vnc = cube;
        vncRecorderLifecycleManager.afterVideoRecordedEvent = event;
        vncRecorderLifecycleManager.stopRecording(after,
            testResult,
            CubeDroneConfiguration.fromMap(conf),
            seleniumContainers
        );

        assertThat(new File(destination,
            "org_arquillian_cube_docker_drone_VncRecorderLifecycleManagerTest_should_discard_recording_if_configured_in_only_failing_and_passed_test.flv"))
            .doesNotExist();
    }

    @Test
    public void should_move_recording_if_configured_in_only_failing_and_failed_test()
        throws IOException, NoSuchMethodException {

        final File destination = temporaryFolder.newFolder("destination");
        final File video = temporaryFolder.newFile("file.flv");

        when(seleniumContainers.getVideoRecordingFile()).thenReturn(video.toPath());
        when(after.getTestClass()).thenReturn(new TestClass(VncRecorderLifecycleManagerTest.class));
        when(after.getTestMethod()).thenReturn(VncRecorderLifecycleManagerTest.class.getMethod(
            "should_move_recording_if_configured_in_only_failing_and_failed_test"));

        Map<String, String> conf = new HashMap<>();
        conf.put("videoOutput", destination.getAbsolutePath());
        conf.put("recordingMode", "ONLY_FAILING");

        TestResult testResult = TestResult.failed(new Throwable());

        VncRecorderLifecycleManager vncRecorderLifecycleManager = new VncRecorderLifecycleManager();
        vncRecorderLifecycleManager.vnc = cube;
        vncRecorderLifecycleManager.afterVideoRecordedEvent = event;
        vncRecorderLifecycleManager.stopRecording(after,
            testResult,
            CubeDroneConfiguration.fromMap(conf),
            seleniumContainers
        );

        assertThat(new File(destination,
            "org_arquillian_cube_docker_drone_VncRecorderLifecycleManagerTest_should_move_recording_if_configured_in_only_failing_and_failed_test.flv"))
            .exists();
    }
}
