package org.arquillian.cube.docker.drone;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Logger;

import org.arquillian.cube.docker.drone.util.SeleniumVersionExtractor;
import org.arquillian.cube.docker.drone.util.VideoFileDestination;
import org.arquillian.cube.docker.drone.util.VolumeCreator;
import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.client.config.BuildImage;
import org.arquillian.cube.docker.impl.client.config.CubeContainer;
import org.arquillian.cube.docker.impl.client.config.Image;
import org.arquillian.cube.docker.impl.client.config.Link;
import org.arquillian.cube.docker.impl.client.config.PortBinding;
import org.arquillian.cube.docker.impl.client.config.StarOperator;
import org.arquillian.cube.docker.impl.util.OperatingSystemFamily;
import org.arquillian.cube.docker.impl.util.OperatingSystemResolver;

public class SeleniumContainers {

    private static final Logger logger = Logger.getLogger(SeleniumContainers.class.getName());

    private static final String SELENIUM_CONTAINER_BASE_NAME = "browser";
    private static final String VNC_CONTAINER_BASE_NAME = "vnc";
    private static final String CONVERSION_CONTAINER_BASE_NAME = "flv2mp4";
    private static final String CHROME_IMAGE = "selenium/standalone-chrome-debug:%s";
    private static final String FIREFOX_IMAGE = "selenium/standalone-firefox-debug:%s";
    private static final String VNC_IMAGE = "richnorth/vnc-recorder:latest";
    private static final String CONVERSION_IMAGE = "arquillian/flv2mp4:0.0.1";
    private static final String DEFAULT_PASSWORD = "secret";
    private static final String VNC_HOSTNAME = "vnchost";
    private static final String VOLUME_DIR = "/recording";
    private static final int VNC_EXPOSED_PORT = 5900;
    public static final String[] FLVREC_COMMAND = new String[] {
        "-o",
        VOLUME_DIR + "/screen.flv",
        "-P",
        VOLUME_DIR + "/password"
        , VNC_HOSTNAME
        , Integer.toString(VNC_EXPOSED_PORT)};

    private CubeContainer seleniumContainer;
    private CubeContainer vncContainer;
    private CubeContainer videoConverterContainer;
    private String browser;
    private Path videoRecordingFolder;
    
    private final String seleniumContainerName;
    private final String vncContainerName;
    private final String conversionContainerName;
    private final int seleniumBoundedPort;

    private SeleniumContainers(String browser, CubeDroneConfiguration cubeDroneConfiguration) {
        this.browser = browser;
        
        switch(cubeDroneConfiguration.getContainerNameStrategy()) {
            case RANDOM:
                UUID uuid = UUID.randomUUID();
                this.seleniumContainerName = StarOperator.generateNewName(SELENIUM_CONTAINER_BASE_NAME, uuid);
                this.vncContainerName = StarOperator.generateNewName(VNC_CONTAINER_BASE_NAME, uuid);
                this.conversionContainerName = StarOperator.generateNewName(CONVERSION_CONTAINER_BASE_NAME, uuid);
                this.seleniumBoundedPort = StarOperator.generateRandomPrivatePort();
                break;
            case STATIC_PREFIX:
                this.seleniumContainerName = cubeDroneConfiguration.getContainerNamePrefix() + "_" + SELENIUM_CONTAINER_BASE_NAME;
                this.vncContainerName = cubeDroneConfiguration.getContainerNamePrefix() + "_" + VNC_CONTAINER_BASE_NAME;
                this.conversionContainerName = cubeDroneConfiguration.getContainerNamePrefix() + "_" + CONVERSION_CONTAINER_BASE_NAME;
                this.seleniumBoundedPort = StarOperator.generateRandomPrivatePort();
                break;
            case STATIC:
            default:
                this.seleniumContainerName = SELENIUM_CONTAINER_BASE_NAME;
                this.vncContainerName = VNC_CONTAINER_BASE_NAME;
                this.conversionContainerName = CONVERSION_CONTAINER_BASE_NAME;
                this.seleniumBoundedPort = 14444;
                break;
        }
        
        this.videoRecordingFolder = VolumeCreator.createTemporaryVolume(DEFAULT_PASSWORD);
        
        this.seleniumContainer = createSeleniumContainer(browser, cubeDroneConfiguration, this.seleniumBoundedPort);
        this.vncContainer = createVncContainer(this.videoRecordingFolder, this.seleniumContainerName);
        final Path targetVolume = VideoFileDestination.resolveTargetDirectory(cubeDroneConfiguration);
        this.videoConverterContainer = createVideoConverterContainer(targetVolume);
        
    }

    public static SeleniumContainers create(String browser, CubeDroneConfiguration cubeDroneConfiguration) {
        return new SeleniumContainers(browser, cubeDroneConfiguration);
    }

    private static CubeContainer createVideoConverterContainer(Path dockerVolume) {

        CubeContainer cubeContainer = new CubeContainer();
        cubeContainer.setImage(Image.valueOf(CONVERSION_IMAGE));

        cubeContainer.setBinds(
            Arrays.asList(convertToBind(dockerVolume, VOLUME_DIR, "rw"))
        );

        // Using log await strategy to match the echo string indicating completion of conversion
        Await await = new Await();

        await.setStrategy("log");
        await.setMatch("CONVERSION COMPLETED");

        cubeContainer.setAwait(await);

        // sets container as manual because we need to start and stop
        cubeContainer.setManual(true);

        return cubeContainer;
    }

    private static CubeContainer createVncContainer(final Path dockerVolume, String seleniumContainerName) {

        CubeContainer cubeContainer = new CubeContainer();
        cubeContainer.setImage(Image.valueOf(VNC_IMAGE));

        cubeContainer.setBinds(
            Arrays.asList(convertToBind(dockerVolume, VOLUME_DIR, "rw"))
        );

        final Link link = Link.valueOf(seleniumContainerName + ":" + VNC_HOSTNAME);
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
    
    private static String convertToBind(Path hostPath, String containterPath, String mode) {
        boolean isWindows = new OperatingSystemResolver().currentOperatingSystem().getFamily() == OperatingSystemFamily.WINDOWS;
        Path absoluteHostPath = hostPath.toAbsolutePath();
        if(isWindows) {
            StringBuilder convertedHostPath = new StringBuilder();
            String hostRoot = absoluteHostPath.getRoot().toString();
            if(hostRoot.matches("[a-zA-Z]:\\\\")) {
                // local path, converts C:\ to /c
                convertedHostPath.append('/');
                convertedHostPath.append(hostRoot.toLowerCase().charAt(0));
            } else {
                // network share, converts \\servername\share\ to /servername/share
                convertedHostPath.append(hostRoot.replace("\\\\", "/").replace("\\", "/"));
                if(convertedHostPath.charAt(convertedHostPath.length() - 1) == '/'){
                    convertedHostPath.deleteCharAt(convertedHostPath.length() - 1);
                }
            }
            
            // join remaining path elements
            for(Path pathElem : absoluteHostPath){
                convertedHostPath.append('/');
                convertedHostPath.append(pathElem.toString());
            }

            if(!absoluteHostPath.startsWith("C:\\Users")) {
                logger.warning(String.format("You're not running below the default shared path 'C:\\Users'. Make sure you have set up a shared folder making the host path '%s' accessible as '%s' in your Docker virtual machine.", absoluteHostPath, convertedHostPath));
            }
            
            return convertedHostPath + ":" + containterPath + ":" + mode;
        }else {
            return absoluteHostPath + ":" + containterPath + ":" + mode;
        }
    }

    private static CubeContainer createSeleniumContainer(String browser, CubeDroneConfiguration cubeDroneConfiguration, int seleniumBoundedPort) {

        if (cubeDroneConfiguration.isBrowserDockerfileDirectorySet()) {
            return createCube(cubeDroneConfiguration.getBrowserDockerfileLocation(), seleniumBoundedPort);
        } else {
            if (cubeDroneConfiguration.isBrowserImageSet()) {
                return configureCube(cubeDroneConfiguration.getBrowserImage(), seleniumBoundedPort);
            } else {
                return useOfficialSeleniumImages(browser, seleniumBoundedPort);
            }
        }
    }

    private static CubeContainer useOfficialSeleniumImages(String browser, int seleniumBoundedPort) {
        String version = SeleniumVersionExtractor.fromClassPath();

        switch (browser) {
            case "firefox":
                return configureCube(String.format(FIREFOX_IMAGE, version), seleniumBoundedPort);
            case "chrome":
                return configureCube(String.format(CHROME_IMAGE, version), seleniumBoundedPort);
            default:
                throw new UnsupportedOperationException(
                    "Unsupported browser " + browser + ". Only firefox and chrome are supported.");
        }
    }

    private static CubeContainer createCube(String dockerFileLocation, int seleniumBoundedPort) {
        CubeContainer cubeContainer = new CubeContainer();
        BuildImage buildImage = new BuildImage(dockerFileLocation, null, true, true);
        cubeContainer.setBuildImage(buildImage);

        setDefaultSeleniumCubeProperties(cubeContainer, seleniumBoundedPort);

        return cubeContainer;
    }

    private static CubeContainer configureCube(String image, int seleniumBoundedPort) {
        CubeContainer cubeContainer = new CubeContainer();
        cubeContainer.setImage(Image.valueOf(image));

        setDefaultSeleniumCubeProperties(cubeContainer, seleniumBoundedPort);

        return cubeContainer;
    }

    private static void setDefaultSeleniumCubeProperties(CubeContainer cubeContainer, int seleniumBoundedPort) {
        cubeContainer.setPortBindings(
            Arrays.asList(PortBinding.valueOf(seleniumBoundedPort + "-> 4444"))
        );

        Await await = new Await();
        await.setStrategy("http");
        await.setResponseCode(getSeleniumExpectedResponseCode());
        await.setUrl("http://dockerHost:" + seleniumBoundedPort);
        cubeContainer.setAwait(await);

        cubeContainer.setKillContainer(true);
    }

    private static int getSeleniumExpectedResponseCode(){
        // Selenium from 3.x onwards returns 200 if started
        int expectedResponseCode = 200;
            String seleniumVersion = SeleniumVersionExtractor.fromClassPath();
            if(seleniumVersion.matches("[0-9]+\\.?.*")){
                int seleniumMajorVersion = Integer.parseInt(seleniumVersion.substring(0, seleniumVersion.indexOf('.')));
                if(seleniumMajorVersion < 3){
                    // Doing an http request to selenium node returns a 403 if Jetty is up and running for Selenium < 3
                    expectedResponseCode = 403;
                }
        }
        return expectedResponseCode;
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
        return seleniumBoundedPort;
    }

    public String getSeleniumContainerName() {
        return seleniumContainerName;
    }

    public String getVncContainerName() {
        return vncContainerName;
    }

    public String getVideoConverterContainerName() {
        return conversionContainerName;
    }

    public String getBrowser() {
        return browser;
    }
}
