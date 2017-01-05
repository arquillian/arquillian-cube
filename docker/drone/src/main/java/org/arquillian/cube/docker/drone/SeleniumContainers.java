package org.arquillian.cube.docker.drone;

import org.arquillian.cube.docker.drone.util.SeleniumVersionExtractor;
import org.arquillian.cube.docker.drone.util.VideoFileDestination;
import org.arquillian.cube.docker.drone.util.VolumeCreator;
import org.arquillian.cube.docker.impl.await.LogScanningAwaitStrategy;
import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.client.config.BuildImage;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.Image;
import org.arquillian.cube.docker.impl.client.config.Link;
import org.arquillian.cube.docker.impl.client.config.PortBinding;

import java.nio.file.Path;
import java.util.Arrays;

public class SeleniumContainers {

    private static final String CHROME_IMAGE = "selenium/standalone-chrome-debug:%s";
    private static final String FIREFOX_IMAGE = "selenium/standalone-firefox-debug:%s";
    private static final String VNC_IMAGE = "richnorth/vnc-recorder:latest";
    private static final String CONVERSION_IMAGE = "flv2mp4";
    private static final String DEFAULT_PASSWORD = "secret";
    private static final String VNC_HOSTNAME = "vnchost";
    private static final String VOLUME_DIR = "recording";

    private static final int SELENIUM_BOUNDED_PORT = 14444;
    private static final int VNC_EXPOSED_PORT = 5900;
    public static final String SELENIUM_CONTAINER_NAME = "browser";
    public static final String VNC_CONTAINER_NAME = "vnc";
    public static final String CONVERSION_CONTAINER_NAME = "flv2mp4";
    public static final String[] FLVREC_COMMAND = new String[]{
            "-o",
            "/" + VOLUME_DIR + "/screen.flv",
            "-P",
            "/" + VOLUME_DIR + "/password"
            , VNC_HOSTNAME
            , Integer.toString(VNC_EXPOSED_PORT)};


    private CubeContainer seleniumContainer;
    private CubeContainer vncContainer;
    private CubeContainer videoConverterContainer;
    private String browser;
    private Path videoRecordingFolder;

    private SeleniumContainers(CubeContainer seleniumContainer, String browser, CubeContainer vncContainer, CubeContainer videoConverterContainer, Path videoRecordingFolder) {
        this.seleniumContainer = seleniumContainer;
        this.vncContainer = vncContainer;
        this.videoConverterContainer = videoConverterContainer;
        this.browser = browser;
        this.videoRecordingFolder = videoRecordingFolder;
    }

    public CubeContainer getSeleniumContainer() {
        return seleniumContainer;
    }

    public CubeContainer getVncContainer() {
        return vncContainer;
    }

    public CubeContainer getVideoConverterContainer() {
        return videoConverterContainer;
    }

    public Path getVideoRecordingFolder() {
        return videoRecordingFolder;
    }

    public Path getVideoRecordingFile() {
        return videoRecordingFolder.resolve("screen.flv");
    }

    public int getSeleniumBoundedPort() {
        return SELENIUM_BOUNDED_PORT;
    }

    public String getSeleniumContainerName() {
        return SELENIUM_CONTAINER_NAME;
    }

    public String getVncContainerName() {
        return VNC_CONTAINER_NAME;
    }

    public String getVideoConverterContainerName() {
        return CONVERSION_CONTAINER_NAME;
    }

    public String getBrowser() {
        return browser;
    }

    public static SeleniumContainers create(String browser, CubeDroneConfiguration cubeDroneConfiguration) {
        final Path temporaryVolume = VolumeCreator.createTemporaryVolume(DEFAULT_PASSWORD);
        final Path targetVolume = VideoFileDestination.resolveTargetDirectory(cubeDroneConfiguration);
        return new SeleniumContainers(createSeleniumContainer(browser, cubeDroneConfiguration), browser, createVncContainer(temporaryVolume), createVideoConverterContainer(targetVolume), temporaryVolume);
    }

    private static CubeContainer createVideoConverterContainer(Path dockerVolume) {


        CubeContainer cubeContainer = new CubeContainer();
        cubeContainer.setImage(Image.valueOf(CONVERSION_IMAGE));

        cubeContainer.setBinds(
                Arrays.asList(dockerVolume.toAbsolutePath().toString() + ":/" + VOLUME_DIR + ":rw")
        );

        //final Link link = Link.valueOf(VNC_HOSTNAME + ":" + CONVERSION_CONTAINER_NAME);
        //cubeContainer.setLinks(Arrays.asList(link));

        // Using log await strategy to match the echo string indicating completion of conversion
        Await await = new Await();
        //LogScanningAwaitStrategy awaitStrategy = new LogScanningAwaitStrategy();

        await.setStrategy("log");
        await.setMatch("CONVERSION COMPLETED");

        cubeContainer.setAwait(await);

        // sets container as manual because we need to start and stop
        cubeContainer.setManual(true);

        return cubeContainer;

    }

    private static CubeContainer createVncContainer(final Path dockerVolume) {

        CubeContainer cubeContainer = new CubeContainer();
        cubeContainer.setImage(Image.valueOf(VNC_IMAGE));

        cubeContainer.setBinds(
                Arrays.asList(dockerVolume.toAbsolutePath().toString() + ":/" + VOLUME_DIR + ":rw")
        );

        final Link link = Link.valueOf(SELENIUM_CONTAINER_NAME + ":" + VNC_HOSTNAME);
        cubeContainer.setLinks(Arrays.asList(link));

        // Using sleeping strategy since VNC client is a CLI without exposing a port
        Await await = new Await();
        await.setStrategy("sleeping");
        await.setSleepTime("100 ms");

        cubeContainer.setAwait(await);

        cubeContainer.setCmd(Arrays.asList(FLVREC_COMMAND));

        // sets container as manual because we need to start and stop for each test case
        cubeContainer.setManual(true);

        return cubeContainer;

    }


    private static CubeContainer createSeleniumContainer(String browser, CubeDroneConfiguration cubeDroneConfiguration) {

        if (cubeDroneConfiguration.isBrowserDockerfileDirectorySet()) {
            return createCube(cubeDroneConfiguration.getBrowserDockerfileLocation());
        } else {
            if (cubeDroneConfiguration.isBrowserImageSet()) {
                return configureCube(cubeDroneConfiguration.getBrowserImage());

            } else {
                return useOfficialSeleniumImages(browser);
            }
        }
    }

    private static CubeContainer useOfficialSeleniumImages(String browser) {
        String version = SeleniumVersionExtractor.fromClassPath();

        switch (browser) {
            case "firefox":
                return configureCube(String.format(FIREFOX_IMAGE, version));
            case "chrome":
                return configureCube(String.format(CHROME_IMAGE, version));
            default:
                throw new UnsupportedOperationException("Only firefox and chrome are supported. Unsupported browser " + browser);
        }
    }

    private static CubeContainer createCube(String dockerFileLocation) {
        CubeContainer cubeContainer = new CubeContainer();
        BuildImage buildImage = new BuildImage(dockerFileLocation, null, true, true);
        cubeContainer.setBuildImage(buildImage);

        setDefaultSeleniumCubeProperties(cubeContainer);

        return cubeContainer;
    }

    private static CubeContainer configureCube(String image) {
        CubeContainer cubeContainer = new CubeContainer();
        cubeContainer.setImage(Image.valueOf(image));

        setDefaultSeleniumCubeProperties(cubeContainer);

        return cubeContainer;
    }

    private static void setDefaultSeleniumCubeProperties(CubeContainer cubeContainer) {
        cubeContainer.setPortBindings(
                Arrays.asList(PortBinding.valueOf(SELENIUM_BOUNDED_PORT + "-> 4444"))
        );

        Await await = new Await();
        await.setStrategy("http");
        // Doing an http request to selenium node returns a 403 if Jetty is up and running
        await.setResponseCode(403);
        await.setUrl("http://dockerHost:" + SELENIUM_BOUNDED_PORT);

        cubeContainer.setAwait(await);

        cubeContainer.setKillContainer(true);

    }

}
