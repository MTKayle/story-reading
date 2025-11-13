package org.example.storyreading.userservice.entity;

import jakarta.persistence.*;
import org.apache.catalina.User;

import java.util.List;

@Entity
@Table(name = "roles")
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String name; // "USER" hoặc "ADMIN"

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    private List<UserEntity> users;

    // --- Constructors ---
//    public Role() {}
//
//    public Role(String name) {
//        this.name = name;
//    }

    // --- Getters & Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<UserEntity> getUsers() {
        return users;
    }

    public void setUsers(List<UserEntity> users) {
        this.users = users;
    }
}
