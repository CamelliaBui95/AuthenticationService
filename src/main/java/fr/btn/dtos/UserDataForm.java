package fr.btn.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDataForm {
    @JsonProperty(index = 1)
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    private String firstName;

    @JsonProperty(index = 2)
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    private String lastName;

    @JsonProperty(index = 3)
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    private LocalDate birthdate;

}
