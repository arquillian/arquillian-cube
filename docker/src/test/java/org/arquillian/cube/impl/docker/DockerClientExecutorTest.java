package org.arquillian.cube.impl.docker;

/**
 * User: BrunoGilbertCrane
 * Date: 20/01/15
 * Time: 5:33 PM
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.arquillian.cube.impl.util.CommandLineExecutor;
import org.arquillian.cube.impl.util.IOUtil;
import org.arquillian.cube.impl.util.OperatingSystemResolver;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DockerClientExecutorTest {

    private static final Pattern HEXA_PATTERN = Pattern.compile("^[A-Fa-f0-9]+$");
    public static final String FULL_LOG = "FROM ubuntu:12.04 ---> f959d044ebdfStep 1 : MAINTAINER Bruno Crane BrunoGilbertCrane@users.noreply.github.com ---> " +
            "Running in dfda3b1da834 ---> f3829d9d380fRemoving intermediate container dfda3b1da834Step 2 : ENV JAVA_HOME /usr/lib/jvm/java-7-openjdk-amd64 ---> " +
            "Running in 0a6b82603339 ---> 92164ef3eeb0Removing intermediate container 0a6b82603339Step 3 : ENV JAVA_OPTS -Dcom.sun.management.jmxremote.port=8089 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false ---> " +
            "Running in d2bab4be218f ---> 5fbf84cc7b1eRemoving intermediate container d2bab4be218fStep 4 : ENV CATALINA_OPTS -Dtomee.host=localhost -Dtomee.http=8081 -Dtomee.shutdown=8015 ---> " +
            "Running in 68c1f0f19014 ---> 61532db01348Removing intermediate container 68c1f0f19014Step 5 : ADD apache-tomee-plus-1.7.1.tar.gz /opt/apache-tomee-plus-1.7.1 ---> 53e8e145f5e0Removing intermediate container 2cf5f4eba0a4Step 6 : WORKDIR /opt/apache-tomee-plus-1.7.1/webapps/ ---> " +
            "Running in 1635c0c6144e ---> 11734dae26a5Removing intermediate container 1635c0c6144eStep 7 : COPY mondrian.war /opt/apache-tomee-plus-1.7.1/webapps/mondrian.war ---> 42634d0d4d5aRemoving intermediate container 610fcecfd374Step 8 : EXPOSE 8081 8015 ---> " +
            "Running in a168700091f6 ---> 31c3e9716369Removing intermediate container a168700091f6Step 9 : CMD /opt/apache-tomee-plus-1.7.1/webapps/bin/catalina.sh run ---> " +
            "Running in 4603bb82ee2c ---> fcff4d43f77aRemoving intermediate container 4603bb82ee2cSuccessfully built fcff4d43f77a";

    @Mock
    private CommandLineExecutor commandLineExecutor;

    @Mock
    private OperatingSystemResolver operatingSystemResolver;

    @Test
    public void testGetImageIdFailed() throws Exception {
        String imageId = IOUtil.substringBetween(FULL_LOG, "Successfully built ", "\\n\"}");
        Assert.assertNull("IOUtil.substringBetween do the job", imageId);
    }

    @Test
    public void testGetImageId() throws Exception {
        String imageId = DockerClientExecutor.getImageId(FULL_LOG);
        Assert.assertNotNull("Pattern matching failed", imageId);

        // All images are identified by a 64 hexadecimal digit string
        // https://docs.docker.com/terms/image/
        Matcher m = HEXA_PATTERN.matcher(imageId);
        Assert.assertTrue("imageId is not an hexadecimal digit string", m.matches());

    }

}
