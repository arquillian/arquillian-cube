package org.arquillian.cube.docker.junit.rule;

import org.arquillian.cube.docker.impl.await.StaticAwaitStrategy;
import org.arquillian.cube.docker.impl.client.config.Await;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.docker.impl.util.OperatingSystemFamily;
import org.arquillian.cube.docker.impl.util.OperatingSystemResolver;
import org.arquillian.cube.requirement.RequirementRule;
import org.awaitility.Awaitility;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verify that {@link ContainerDslRule} can be used to build a valid {@code PosatgreSql} Docker container.
 * <p>
 *     A {@link ContainerDslRule} is used to define a PostgreSql Docker container.
 *     A local directory is created on the host to hold the PostgreSql data and to be mounted as a volume on the
 *     Docker container.
 *     The data directory is created statically, so that it can be used by the {@link ContainerDslRule} definition later
 *     in the test lifecycle.
 *     The {@code Z} bind mode option is passed so that it can be used by SELinux enabled systems, see the related
 *     <a href="https://docs.docker.com/engine/storage/bind-mounts/#configure-the-selinux-label">Docker documentation</a>,
 *     otherwise the container would fail to start with the following error:<br>
 *     {@code mkdir: cannot create directory '/var/lib/pgsql/data/userdata': Permission denied}
 * </p>
 */
@Category({ RequiresDocker.class})
public class PostgreSqlIT {
    static final String POSTGRESQL_USER = "user";
    static final String POSTGRESQL_PASSWORD = "pass";
    static final String POSTGRESQL_DATABASE = "test-database";
    static final String POSTGRESQL_IMAGE = "quay.io/centos7/postgresql-13-centos7:centos7";
    static final int POSTGRESQL_HOST_PORT_BINDING = 5432;
    static final String POSTGRESQL_CONTAINER_DATA_DIRECTORY_PATH = "/var/lib/pgsql/data";
    static final File POSTGRESQL_HOST_DATA_DIRECTORY;

    static {
        try {
            POSTGRESQL_HOST_DATA_DIRECTORY = createPostgresDataDir();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create PostgreSql data dir", e);
        }
    }

    private static File createPostgresDataDir() throws IOException {
        // create local data dir for postgres database
        File postgresDataDir = java.nio.file.Files.createTempDirectory(null).toFile();
        final boolean isLinux = new OperatingSystemResolver().currentOperatingSystem().getFamily() == OperatingSystemFamily.LINUX;
        if (isLinux) {
            // This is needed to run on SELinux enabled Docker hosts
            // otherwise the following error will prevent the container from starting successfully:
            //   mkdir: cannot create directory '/var/lib/pgsql/data/userdata': Permission denied
            java.nio.file.Files.setPosixFilePermissions(Path.of(postgresDataDir.toURI()),
                PosixFilePermissions.fromString("rwxrwxrwx"));
        }
        return postgresDataDir;
    }

    private static Await postgreSqlContainerStaticAwaitStrategy() {
        Await await = new Await();
        await.setStrategy(StaticAwaitStrategy.TAG);
        await.setIp("localhost");
        await.setPorts(List.of(5432));
        return await;
    }

    @Rule
    public RequirementRule requirementRule = new RequirementRule();

    @ClassRule
    public static ContainerDslRule postgres =
        new ContainerDslRule(POSTGRESQL_IMAGE, "postgres")
            .withPortBinding(String.format("%s->5432", POSTGRESQL_HOST_PORT_BINDING))
            .withEnvironment("POSTGRESQL_DATABASE", POSTGRESQL_DATABASE)
            .withEnvironment("POSTGRESQL_USER", POSTGRESQL_USER)
            .withEnvironment("POSTGRESQL_PASSWORD", POSTGRESQL_PASSWORD)
            .withVolume(POSTGRESQL_HOST_DATA_DIRECTORY.getAbsolutePath(), POSTGRESQL_CONTAINER_DATA_DIRECTORY_PATH, "Z")
            .withAwaitStrategy(postgreSqlContainerStaticAwaitStrategy());

    /**
     * Verify that the {@code PostgreSql} Docker container is up and running by looking into an expected message in the
     * container logs, which are retrieved via a call to {@link ContainerDslRule#getLog()}.
     */
    @Test
    public void postgres_should_be_up_and_running() {
        Awaitility.await()
            .atMost(30, TimeUnit.SECONDS)
            .untilAsserted(() -> assertThat(postgres.getLog())
                .isNotBlank()
                .contains("Success. You can now start the database server using:")
            );
    }
}
