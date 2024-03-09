package fr.btn.resources;

import fr.btn.dtos.MailClient;
import fr.btn.dtos.NewUser;
import fr.btn.dtos.ResponseUser;
import fr.btn.entities.UserEntity;
import fr.btn.repositories.UserRepository;
import fr.btn.services.MailService;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {
    private static final String API_KEY = "TEwLHA9MSQ1UFJzcHVScmJzVStaMllpaXQzYUNBJGRWdTIyY3hyb0Q1ZGdn";
    @Inject
    @RestClient
    MailService mailService;

    @Inject
    UserRepository userRepository;

    @POST
    @Path("/register")
    @PermitAll
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(NewUser newUser) {
        if(newUser == null)
            return Response.status(Response.Status.BAD_REQUEST).build();

        if(!isEmailValid(newUser.getEmail()))
            return Response.ok("Invalid Email.").status(Response.Status.BAD_REQUEST).build();

        if(!isUsernameValid(newUser.getUsername()))
            return Response.ok("Invalid Username.").status(Response.Status.BAD_REQUEST).build();

        UserEntity userEntity = NewUser.toUserEntity(newUser);

        userRepository.persist(userEntity);

        sendMail(newUser.getEmail(), "New User", "Click this link to activate your account");

        return Response.ok(new ResponseUser(userEntity)).status(Response.Status.CREATED).build();
    }

    private boolean isEmailValid(String email) {
        return email != null && !email.isEmpty() && userRepository.count("email=?1", email) == 0;
    }

    private boolean isUsernameValid(String username) {
        return username != null && !username.isEmpty() && userRepository.count("username=?1", username) == 0;
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
