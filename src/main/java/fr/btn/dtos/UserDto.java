package fr.btn.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.btn.entities.UserEntity;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class UserDto {
    @JsonProperty(index = 1)
    private Integer id;

    @JsonProperty(index = 2)
    private String firstName;

    @JsonProperty(index = 3)
    private String lastName;

    @JsonProperty(index = 4)
    private String username;

    @JsonProperty(index = 5)
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    private LocalDate birthdate;

    @JsonProperty(index = 6)
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    private String role;

    public UserDto(UserEntity userEntity, boolean withRole) {
        this.id = userEntity.getId();
        this.firstName = userEntity.getFirstName();
        this.lastName = userEntity.getLastName();
        this.username = userEntity.getUsername();
        this.birthdate = userEntity.getBirthdate();

        if(withRole)
            this.role = userEntity.getRole();
    }

    public static List<UserDto> toDtoList(List<UserEntity> userEntities) {
        List<UserDto> userDtos = new ArrayList<>();

        for(UserEntity userEntity : userEntities)
            userDtos.add(new UserDto(userEntity, false));

        return userDtos;
    }
}
