package org.arquillian.cube.persistence;

import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@Category({RequiresDocker.class})
@RunWith(ArquillianConditionalRunner.class)
public class EmployeeRepositoryIT {

    @Inject
    private EmployeeRepository repository;

    @Deployment
    public static WebArchive create() {
        return ShrinkWrap.create(WebArchive.class)
            .addClasses(Employee.class, EmployeeRepository.class)
            .addAsResource("test-persistence.xml", "META-INF/persistence.xml")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsLibraries(Maven.configureResolver()
                .loadPomFromFile("pom.xml")
                .resolve("org.postgresql:postgresql:9.3-1102-jdbc41", "org.arquillian.cube:arquillian-cube-requirement")
                .withTransitivity().asFile());
    }

    @Test
    public void shouldStoreUser() throws IOException {
        repository.store(new Employee("test"));

        final List<Employee> allEmployees = repository.findAllUsers();
        Assert.assertEquals(allEmployees.size(), 1);
    }

}
