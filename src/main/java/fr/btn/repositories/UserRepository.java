package fr.btn.repositories;

import fr.btn.entities.UserEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class UserRepository implements PanacheRepositoryBase<UserEntity, Integer> {
    public UserEntity findUserByUsername(String username) {
        return find("username=?1", username).firstResult();
    }

    public UserEntity findUserByEmail(String email) {
        return find("email=?1", email).firstResult();
    }

    public long countByUsername(String username) {
        return count("username=?1", username);
    }
}
