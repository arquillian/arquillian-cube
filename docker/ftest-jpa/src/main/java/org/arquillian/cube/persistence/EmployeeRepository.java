package org.arquillian.cube.persistence;

import java.util.List;
import javax.annotation.sql.DataSourceDefinition;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

@DataSourceDefinition(name = "java:app/datasources/postgresql_ds",
    className = "org.postgresql.ds.PGSimpleDataSource",
    //url = "jdbc:postgresql://postgres:5432/", // when running mvn clean install -Pwildfly-12-remote
    url = "jdbc:postgresql://localhost:5433/",
    databaseName = "test_database",
    user = "postgres",
    password = "postgres")
@Singleton
@Startup
public class EmployeeRepository {
    @PersistenceContext
    private EntityManager em;

    public void store(Employee employee) {
        em.persist(employee);
    }

    public List<Employee> findAllUsers() {
        Query query = em.createQuery("FROM Employee");
        List<Employee> employeeList = query.getResultList();
        return employeeList;
    }
}
