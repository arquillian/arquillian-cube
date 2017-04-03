package org.arquillian.cube.persistence;

import java.io.IOException;
import javax.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class UserRepositoryTest {

    @Inject
    private UserRepository repository;

    @Deployment
    public static WebArchive create() {
        return ShrinkWrap.create(WebArchive.class)
            .addClasses(User.class, UserRepository.class, UserRepositoryTest.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsResource("test-persistence.xml", "META-INF/persistence.xml")
            .addAsManifestResource(new StringAsset("Dependencies: com.h2database.h2\n"), "MANIFEST.MF");
    }

    @Test
    public void shouldStoreUser() throws IOException {
        repository.store(new User("test"));
    }
}
