package fr.btn.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewPasswordRqForm {
    @JsonProperty(index = 1)
    private String password;

    @JsonProperty(index = 2)
    private String newPassword;
}
