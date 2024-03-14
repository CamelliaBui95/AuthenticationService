package fr.btn.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name="UTILISATEUR")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="ID_UTILISATEUR")
    private Integer id;

    @Column(name = "PRENOM")
    private String firstName;

    @Column(name = "NOM")
    private String lastName;

    @Column(name = "DATE_NAISSANCE")
    private LocalDate birthdate;

    @Column(name="EMAIL")
    private String email;

    @Column(name="LOGIN")
    private String username;

    @Column(name="PASSWORD")
    private String password;

    @Column(name="ROLE")
    private String role;

    @Column(name="STATUS")
    private String status;

    @Column(name="CODE_PIN")
    private Integer pinCode;

    @Column(name="LAST_ACCESS")
    private LocalDateTime lastAccess;

    @Column(name="NBRE_FAIL_ATTEMPTS")
    private Integer numFailAttempts;
}
