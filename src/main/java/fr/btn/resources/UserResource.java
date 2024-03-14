package fr.btn.resources;

import fr.btn.dtos.*;
import fr.btn.entities.UserEntity;
import fr.btn.repositories.UserRepository;
import fr.btn.securityUtils.Argon2;
import fr.btn.services.MailService;
import fr.btn.utils.Utils;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SecurityScheme(
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT"
)
@Path("/user")
@Tag(name="User")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {
    private static final String API_KEY = "TEwLHA9MSQ1UFJzcHVScmJzVStaMllpaXQzYUNBJGRWdTIyY3hyb0Q1ZGdn";

    @Context
    UriInfo request;

    @Inject
    @RestClient
    MailService mailService;

    @Inject
    UserRepository userRepository;

    @Inject
    JsonWebToken jwt;

    private Set<String> statusConstraints;

    public UserResource() {
        this.statusConstraints = new HashSet<>();

        statusConstraints.add("PENDING");
        statusConstraints.add("INACTIVE");
    }

    @POST
    @Path("{username}/request_reset_password")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional
    @PermitAll
    public Response requestResetPassword(@PathParam("username") String username, String newPassword) {
        if(username == null || username.isEmpty() || newPassword == null || newPassword.isEmpty())
            return Response.ok("Username or New Password is invalid.").status(Response.Status.BAD_REQUEST).build();

        UserEntity existingUser = userRepository.findUserByUsername(username);
        // Send link to register endpoint here
        if(existingUser == null || statusConstraints.contains(existingUser.getStatus()))
            return Response.status(Response.Status.NOT_FOUND).build();

        String hashedPassword = Argon2.getHashedPassword(newPassword);

        List<String> data = Arrays.asList(username, hashedPassword);
        String confirmCode = Utils.generateEncodedStringWithUserData(data);

        URI uri = UriBuilder
                .fromUri(request.getBaseUri())
                .path("auth/reset_password")
                .queryParam("code", confirmCode).build();

        sendMail(existingUser.getEmail(), "Confirm to reset password", uri.toString());

        existingUser.setStatus("LOCKED");

        return Response.ok("Please confirm your request to reset password by clicking on the link that has been sent to your email.").build();
    }

    @POST
    @Path("/forgot_login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    @PermitAll
    public Response requestUsername(UsernameRequestForm form) {
        if(form == null || form.getEmail() == null|| form.getPassword() == null)
            return Response.ok("Invalid data.").status(Response.Status.BAD_REQUEST).build();

        String email = form.getEmail();
        String password = form.getPassword();

        UserEntity existingUser = userRepository.findUserByEmail(email);
        if(existingUser == null || statusConstraints.contains(existingUser.getStatus()))
            return Response.status(Response.Status.NOT_FOUND).build();

        if(!Argon2.validate(password, existingUser.getPassword()))
            return Response.ok("Incorrect Password.").status(Response.Status.NOT_ACCEPTABLE).build();

        sendMail(existingUser.getEmail(), "Your username", existingUser.getUsername());

        return Response.ok("Please check your email to retrieve your username.").build();
    }

    @POST
    @Path("{username}/request_modify_password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    @RolesAllowed({"USER", "ADMIN"})
    public Response modifyPassword(@PathParam("username") String username, NewPasswordRqForm form) {
        if(form == null || form.getPassword() == null || form.getNewPassword() == null || form.getNewPassword().isEmpty())
            return Response.ok("Invalid data.").status(Response.Status.BAD_REQUEST).build();

        if(!username.equals(jwt.getSubject()))
            return Response.ok("Invalid username.").status(Response.Status.BAD_REQUEST).build();

        String currentPassword = form.getPassword();
        String newPassword = form.getNewPassword();

        UserEntity existingUser = userRepository.findUserByUsername(username);
        if(existingUser == null || statusConstraints.contains(existingUser.getStatus()))
            return Response.status(Response.Status.NOT_FOUND).build();

        if(!Argon2.validate(currentPassword, existingUser.getPassword()))
            return Response.ok("Incorrect Password.").status(Response.Status.NOT_ACCEPTABLE).build();

        return requestResetPassword(username, newPassword);
    }

    @PUT
    @Path("{username}/modify_data")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    @RolesAllowed({"USER", "ADMIN"})
    public Response modifyUserData(@PathParam("username") String username, UserDataForm dataForm) {
        if(!username.equals(jwt.getSubject()))
            return Response.ok("Invalid username.").status(Response.Status.BAD_REQUEST).build();

        if(dataForm == null)
            return Response.ok("Invalid data.").status(Response.Status.BAD_REQUEST).build();

        UserEntity existingUser = userRepository.findUserByUsername(username);

        if(existingUser == null || statusConstraints.contains(existingUser.getStatus()))
            return Response.status(Response.Status.NOT_FOUND).build();

        String firstName = dataForm.getFirstName();
        String lastName = dataForm.getLastName();
        String newUsername = dataForm.getUsername();
        LocalDate birthdate = dataForm.getBirthdate();

        if(firstName != null && !firstName.isEmpty())
            existingUser.setFirstName(firstName);
        if(lastName != null && !lastName.isEmpty())
            existingUser.setLastName(lastName);
        if (isUsernameValid(newUsername))
            existingUser.setUsername(newUsername);
        if(birthdate != null)
            existingUser.setBirthdate(birthdate);

        return Response.ok(new UserDto(existingUser, false)).build();
    }

    private boolean sendMail(String recipient, String subject, String content) {
        MailClient mailClient = MailClient
                .builder()
                .recipient(recipient)
                .subject(subject)
                .content(content)
                .build();

        try (Response response = mailService.send(API_KEY, mailClient)) {
            if (response.getStatus() == 500)
                return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean isUsernameValid(String username) {
        if(username == null || username.isEmpty())
            return false;

        UserEntity foundUser = userRepository.findUserByUsername(username);

        if(foundUser != null && foundUser.getStatus().equals("PENDING"))
            return userRepository.deleteById(foundUser.getId());

        return foundUser == null;
    }

}

// Forgot Login : Login Endpoint verifies email:
    // if email is valid => send a password reset link with expiration to the email