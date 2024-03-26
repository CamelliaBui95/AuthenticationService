package fr.btn.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailClient {
    @JsonProperty(index = 1)
    private String recipient;
    @JsonProperty(index = 2)
    private String subject;
    @JsonProperty(index = 3)
    private String content;

    @JsonProperty(index = 4)
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    private LocalDate date;

    @JsonProperty(index = 5)
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    private LocalTime time;
}
