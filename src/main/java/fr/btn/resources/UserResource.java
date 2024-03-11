package fr.btn.resources;

import fr.btn.dtos.MailClient;
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
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SecurityScheme(
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT"
)
@Path("/users")
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

    @GET
    @Path("/all")
    @RolesAllowed("ADMIN")
    public void getUsers() {
        System.out.println("Accessed");
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
        if(existingUser == null || existingUser.getStatus().equals("INACTIVE"))
            return Response.ok("User does not exist.").status(Response.Status.NOT_FOUND).build();

        String hashedPassword = Argon2.getHashedPassword(newPassword);

        List<String> data = Arrays.asList(username, hashedPassword);
        String confirmCode = Utils.generateEncodedStringWithUserData(data);

        URI uri = UriBuilder
                .fromUri(request.getBaseUri())
                .path("auth/reset_password")
                .queryParam("code", confirmCode).build();

        sendMail(existingUser.getEmail(), "Confirm to reset password", uri.toString());

        existingUser.setConfirmDateTime(LocalDateTime.now());
        existingUser.setStatus("LOCKED");

        return Response.ok("Please confirm your request to reset password by clicking on the link that has been sent to your email.").build();
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