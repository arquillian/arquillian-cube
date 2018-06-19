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
    url = "jdbc:postgresql://postgres:5432/",
    databaseName = "test_database",
    user = "postgres",
    password = "postgres")
@Singleton
@Startup
public class UserRepository {

    @PersistenceContext
    private EntityManager em;

    public void store(User user) {
        em.persist(user);
    }

    public List<User> findAllUsers() {
        Query query = em.createQuery("FROM User");
        List<User> userList = query.getResultList();
        return userList;
    }
}
