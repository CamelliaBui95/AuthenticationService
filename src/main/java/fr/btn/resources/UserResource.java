package fr.btn.resources;

import fr.btn.dtos.MailClient;
import fr.btn.dtos.NewUser;
import fr.btn.dtos.ResponseUser;
import fr.btn.entities.UserEntity;
import fr.btn.repositories.UserRepository;
import fr.btn.securityUtils.Cryptographer;
import fr.btn.services.MailService;
import io.vertx.ext.auth.User;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {
    @Context
    UriInfo request;

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

        String encodedActivationStr = generateEncodedAccountActivationStr(userEntity);
        URI uri = UriBuilder
                .fromUri(request.getBaseUri())
                .path("users/confirm")
                .queryParam("code", encodedActivationStr).build();

        sendMail(newUser.getEmail(), "Account Activation", uri.toString());

        return Response.ok(new ResponseUser(userEntity)).status(Response.Status.CREATED).build();
    }

    @GET
    @Transactional
    @Path("/confirm")
    public Response confirmAccount(@QueryParam("code") String encodedData) {
        try {
            List<String> userData = decodeAndExtractData(encodedData);

            if(!isActivationCodeValid(userData))
                return Response.ok("This code is expired.").status(Response.Status.NOT_ACCEPTABLE).build();

            UserEntity user = userRepository.find("username=?1", userData.get(0)).firstResult();

            user.setStatus("ACTIVE");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Response.ok().build();
    }


    /*@GET
    @Path("/test")
    public void displayUri() {
        URI uri = UriBuilder.fromUri(request.getBaseUri()).path("users/confirm").queryParam("code", "value").build(); // == http://localhost:8085/users/confirm?code=value
        System.out.println(uri);
    }*/

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

    private String generateEncodedAccountActivationStr(UserEntity user) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 30);

        long expInMilliseconds = calendar.getTimeInMillis();
        String username = user.getUsername();
        String role = user.getRole();
        try {
            String encodedData = Cryptographer.encode(String.format("%s|%s|%s|", username, role, expInMilliseconds));

            return encodedData;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    private List<String> decodeAndExtractData(String encodedData) {
        try {
            String decodedData = Cryptographer.decode(encodedData);

            StringBuilder builder = new StringBuilder();
            List<String> parts = new ArrayList<>();

            int p = 0;
            while(p < decodedData.length()) {
                if(decodedData.charAt(p) == '|') {
                    String word = builder.toString();
                    parts.add(word);
                    builder.delete(0, word.length());
                }
                else
                    builder.append(decodedData.charAt(p));

                p++;
            }

            return parts;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean isActivationCodeValid(List<String> userData) {
        if(userData.size() != 3)
            return false;

        try {
            String username = userData.get(0);
            long expInMilliSecs = Long.parseLong(userData.get(2));

            Calendar calendar = Calendar.getInstance();
            long nowInMilliSecs = calendar.getTimeInMillis();

            if(expInMilliSecs - nowInMilliSecs <= 0)
                return false;

            return userRepository.count("username=?1", username) == 1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }
}
