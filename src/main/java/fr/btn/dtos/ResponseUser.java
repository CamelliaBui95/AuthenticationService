package fr.btn.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.btn.entities.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseUser {
    @JsonProperty(index = 1)
    private Integer id;

    @JsonProperty(index = 2)
    private String firstName;

    @JsonProperty(index = 3)
    private String lastName;

    @JsonProperty(index = 4)
    private String username;

    @JsonProperty(index = 5)
    private LocalDate birthdate;

    @JsonProperty(index = 6)
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    private String role;

    public ResponseUser(UserEntity userEntity, boolean withRole) {
        this.id = userEntity.getId();
        this.firstName = userEntity.getFirstName();
        this.lastName = userEntity.getLastName();
        this.username = userEntity.getUsername();
        this.birthdate = userEntity.getBirthdate();

        if(withRole)
            this.role = userEntity.getRole();
    }
}
