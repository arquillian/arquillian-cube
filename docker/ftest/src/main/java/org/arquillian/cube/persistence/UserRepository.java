package org.arquillian.cube.persistence;

import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@DataSourceDefinition(
    name = "java:app/TestDataSource",
    className = "org.h2.jdbcx.JdbcDataSource",
    url = "jdbc:h2:tcp://database:1521/opt/h2-data/test",
    user = "sa",
    password = "sa"
)
@Stateless
public class UserRepository {

    @PersistenceContext
    private EntityManager em;

    public void store(User user) {
        em.persist(user);
    }
}
