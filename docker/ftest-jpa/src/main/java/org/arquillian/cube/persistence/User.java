package org.arquillian.cube.persistence;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class User {

    @Id
    private String id;

    private String name;

    protected User() {
    }

    public User(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
