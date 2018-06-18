package org.arquillian.cube.persistence;

import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class UserRepositoryTest {

    @Inject
    private UserRepository repository;

    @Deployment
    public static WebArchive create() {
        final File[] files = Maven.configureResolver()
            .loadPomFromFile("pom.xml")
            .resolve("org.postgresql:postgresql:9.3-1102-jdbc41")
            .withTransitivity()
            .asFile();

        return ShrinkWrap.create(WebArchive.class)
            .addAsLibraries(files)
            .addClasses(User.class, UserRepository.class, UserRepositoryTest.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsResource("test-persistence.xml", "META-INF/persistence.xml");
    }

    @Test
    public void shouldStoreUser() throws IOException {
        repository.store(new User("test"));

        final List<User> allUsers = repository.findAllUsers();
        Assert.assertEquals(allUsers.size(), 1);
    }

}
