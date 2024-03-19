package fr.btn.resources;

import fr.btn.dtos.*;
import fr.btn.entities.UserEntity;
import fr.btn.hateos.HateOs;
import fr.btn.repositories.UserRepository;
import fr.btn.securityUtils.Argon2;
import fr.btn.services.MailService;
import fr.btn.utils.Utils;
import fr.btn.utils.Validator;
import io.vertx.ext.auth.User;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.config.ConfigProvider;
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
@Path("/users")
@Tag(name="User")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {
    private static final String API_KEY = ConfigProvider.getConfig().getValue("app-config.api-key", String.class);

    @Context
    UriInfo request;

    @Inject
    @RestClient
    MailService mailService;

    @Inject
    UserRepository userRepository;

    @Inject
    JsonWebToken jwt;

    @PUT
    @Path("/promote")
    @RolesAllowed("ADMIN")
    public Response promoteUser(@QueryParam("username") String username) {
        if(!Validator.validateUsername(username))
            return Response.ok("Invalid Username.").status(Response.Status.BAD_REQUEST).build();

        UserEntity existingUser = userRepository.findUserByUsername(username);
        if(existingUser == null)
            return Response.ok("User not found.").status(Response.Status.NOT_FOUND).build();

        existingUser.setRole("ADMIN");

        return Response.ok().build();
    }

    @GET
    @Path("/all_users")
    @RolesAllowed("ADMIN")
    public Response getAll() {
        List<UserEntity> userEntities = userRepository.listAll();

        return Response.ok(UserDto.toDtoList(userEntities)).build();
    }

    @POST
    @Path("{username}/forgot_password")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Response requestResetPassword(@PathParam("username") String username, @FormParam("new_password") String newPassword) {
        if(!Validator.validatePassword(newPassword))
            return Response.ok("Password is invalid.").status(Response.Status.BAD_REQUEST).build();

        UserEntity existingUser = userRepository.findUserByUsername(username);
        // Send link to register endpoint here

//        UriBuilder builder = UriBuilder.fromUri(request.getBaseUri());
//        HateOs hateOs = new HateOs();
//
//        hateOs.addLink("Forgot Username", HttpMethod.GET, builder.build());

        if(existingUser == null)
            return Response.ok("User does not exist.").status(Response.Status.NOT_FOUND).build();

        Response canAccessRes = Validator.validateAccess(existingUser);
        if(canAccessRes.getStatus() != 200)
            return canAccessRes;

        String hashedPassword = Argon2.getHashedPassword(newPassword);

        List<String> data = Arrays.asList(username, hashedPassword);
        String confirmCode = Utils.generateEncodedStringWithUserData(data);

        URI uri = UriBuilder
                .fromUri(request.getBaseUri())
                .path("auth/confirm_reset_password")
                .queryParam("code", confirmCode).build();

        sendMail(existingUser.getEmail(), "Confirm to reset password", uri.toString());

        return Response.ok("Please confirm your request to reset password by clicking on the link that has been sent to your email.").build();
    }

    @POST
    @Path("/forgot_username")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Response requestUsername(@FormParam("email") String email) {
        if(!Validator.validateEmail(email))
            return Response.ok("A valid email is required.").status(Response.Status.BAD_REQUEST).build();

        UserEntity existingUser = userRepository.findUserByEmail(email);
        if(existingUser == null)
            return Response.ok("User not found.").status(Response.Status.NOT_FOUND).build();

        sendMail(existingUser.getEmail(), "Your username", existingUser.getUsername());

        return Response.ok("Please check your email to retrieve your username.").build();
    }

    @POST
    @Path("{username}/modify_password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    @RolesAllowed({"USER", "ADMIN"})
    public Response modifyPassword(@PathParam("username") String username, @FormParam("password") String password ,@FormParam("new_password") String newPassword) {
        if(!Validator.validateUsername(username))
            return Response.ok("Invalid Username.").status(Response.Status.BAD_REQUEST).build();

        if(!Validator.validatePassword(newPassword))
            return Response.ok("New password is invalid.").status(Response.Status.BAD_REQUEST).build();

        UserEntity existingUser = userRepository.findUserByUsername(username);
        if(existingUser == null)
            return Response.ok("User not found.").status(Response.Status.NOT_FOUND).build();

        Response canAccessRes = Validator.validateAccess(existingUser);
        if(canAccessRes.getStatus() != 200)
            return canAccessRes;

        if(!Argon2.validate(password, existingUser.getPassword())) {
            int numFails = existingUser.getNumFailAttempts() == null ? 0 : existingUser.getNumFailAttempts();
            existingUser.setNumFailAttempts(numFails + 1);
            existingUser.setLastAccess(LocalDateTime.now());

            return Response.ok("Incorrect Password.").status(Response.Status.FORBIDDEN).build();
        }
        
        return requestResetPassword(username, newPassword);
    }

    @PUT
    @Path("{username}/modify_data")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    @RolesAllowed({"USER", "ADMIN"})
    public Response modifyUserData(@PathParam("username") String username, UserDataForm dataForm) {
        if(!Validator.validateUsername(username) || !jwt.getSubject().equals(username))
            return Response.ok("Invalid Username.").status(Response.Status.BAD_REQUEST).build();

        if(dataForm == null)
            return Response.ok("Invalid data.").status(Response.Status.BAD_REQUEST).build();

        UserEntity existingUser = userRepository.findUserByUsername(username);
        if(existingUser == null)
            return Response.status(Response.Status.NOT_FOUND).build();

        Response canAccessRes = Validator.validateAccess(existingUser);
        if(canAccessRes.getStatus() != 200)
            return canAccessRes;

        String firstName = dataForm.getFirstName();
        String lastName = dataForm.getLastName();
        LocalDate birthdate = dataForm.getBirthdate();

        if(firstName != null && !firstName.isEmpty())
            existingUser.setFirstName(firstName);
        if(lastName != null && !lastName.isEmpty())
            existingUser.setLastName(lastName);
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

}

// Forgot Login : Login Endpoint verifies email:
    // if email is valid => send a password reset link with expiration to the email