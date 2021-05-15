package io.github.ulisse1996.jaorm.integration.test.entity;

import io.github.ulisse1996.jaorm.annotation.Column;
import io.github.ulisse1996.jaorm.annotation.Id;
import io.github.ulisse1996.jaorm.annotation.Relationship;
import io.github.ulisse1996.jaorm.annotation.Table;
import io.github.ulisse1996.jaorm.entity.Result;

import java.util.List;
import java.util.Objects;

@Table(name = "USER_ENTITY")
public class User {

    @Id
    @Column(name = "USER_ID")
    private int id;

    @Column(name = "USER_NAME")
    private String name;

    @Column(name = "DEPARTMENT_ID")
    private int departmentId;

    @Relationship(
            columns = @Relationship.RelationshipColumn(targetColumn = "USER_ID", sourceColumn = "USER_ID")
    )
    private List<UserRole> roles;

    @Relationship(
            columns = @Relationship.RelationshipColumn(targetColumn = "USER_ID", sourceColumn = "USER_ID")
    )
    private Result<UserSpecific> userSpecific;

    public Result<UserSpecific> getUserSpecific() {
        return userSpecific;
    }

    public void setUserSpecific(Result<UserSpecific> userSpecific) {
        this.userSpecific = userSpecific;
    }

    public List<UserRole> getRoles() {
        return roles;
    }

    public void setRoles(List<UserRole> roles) {
        this.roles = roles;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(int departmentId) {
        this.departmentId = departmentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id && departmentId == user.departmentId && Objects.equals(name, user.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, departmentId);
    }
}
