package fr.btn.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.btn.entities.UserEntity;
import fr.btn.securityUtils.Argon2;
import io.vertx.ext.auth.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewUser {
    @JsonProperty(index = 1)
    private String firstName;

    @JsonProperty(index = 2)
    private String lastName;

    @JsonProperty(index = 3)
    private LocalDate birthdate;

    @JsonProperty(index = 4)
    private String email;

    @JsonProperty(index = 5)
    private String username;

    @JsonProperty(index = 6)
    private String password;

    @JsonProperty(index = 7)
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    private String role;

    public static UserEntity toUserEntity(NewUser newUser) {
        return UserEntity
                .builder()
                .role(newUser.role.isEmpty() ? "USER" : newUser.role)
                .firstName(newUser.firstName)
                .lastName(newUser.lastName)
                .birthdate(newUser.birthdate)
                .email(newUser.email)
                .username(newUser.username)
                .password(Argon2.getHashedPassword(newUser.password))
                .status("INACTIVE")
                .build();
    }
}
