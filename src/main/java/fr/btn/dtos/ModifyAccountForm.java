package fr.btn.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifyAccountForm {
    @JsonProperty(index = 1)
    private Integer userId;

    @JsonProperty(index = 2)
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    private String role;

    @JsonProperty(index = 3)
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    private String status;
}
