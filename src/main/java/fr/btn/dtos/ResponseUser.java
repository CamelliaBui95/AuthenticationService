package fr.btn.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.btn.entities.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseUser {
    @JsonProperty(index = 1)
    private Integer id;

    @JsonProperty(index = 2)
    private String username;

    @JsonProperty(index = 3)
    private String email;

    public ResponseUser(UserEntity userEntity) {
        this.id = userEntity.getId();
        this.username = userEntity.getUsername();
        this.email = userEntity.getEmail();
    }
}
