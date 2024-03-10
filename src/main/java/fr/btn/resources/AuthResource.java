package fr.btn.resources;

import fr.btn.dtos.MailClient;
import fr.btn.dtos.NewUser;
import fr.btn.dtos.ResponseUser;
import fr.btn.entities.UserEntity;
import fr.btn.repositories.UserRepository;
import fr.btn.securityUtils.Cryptographer;
import fr.btn.securityUtils.TokenUtil;
import fr.btn.services.MailService;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {
    @Context
    UriInfo request;

    private static final String API_KEY = "TEwLHA9MSQ1UFJzcHVScmJzVStaMllpaXQzYUNBJGRWdTIyY3hyb0Q1ZGdn";

    private static final int EXP_IN_MILLIS = 3 * 60 * 1000;
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
        userEntity.setConfirmDateTime(LocalDateTime.now());

        URI uri = UriBuilder
                .fromUri(request.getBaseUri())
                .path("auth/confirm")
                .queryParam("code", encodedActivationStr).build();

        sendMail(newUser.getEmail(), "Account Activation", uri.toString());

        return Response.ok(new ResponseUser(userEntity)).status(Response.Status.CREATED).build();
    }

    public Response login() {
        return null;
    }

    @GET
    @Transactional
    @PermitAll
    @Path("/confirm")
    public Response confirmAccount(@QueryParam("code") String encodedData) {
        try {
            List<String> userData = decodeAndExtractData(encodedData);

            if(!isActivationCodeValid(userData))
                return Response.ok("This code is expired.").status(Response.Status.NOT_ACCEPTABLE).build();

            UserEntity user = userRepository.find("username=?1", userData.get(0)).firstResult();

            user.setStatus("ACTIVE");
            user.setConfirmDateTime(null);

            String token = TokenUtil.generateJwt(user.getUsername(), user.getRole());

            return Response.ok().header(HttpHeaders.AUTHORIZATION, token).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    private boolean isEmailValid(String email) {
        if(email == null || email.isEmpty())
            return false;

        UserEntity foundUser = userRepository.find("email=?1", email).firstResult();

        if(foundUser != null && foundUser.getStatus().equals("INACTIVE") && isAccountExpired(foundUser))
            return userRepository.deleteById(foundUser.getId());

        return foundUser == null;
    }

    private boolean isUsernameValid(String username) {
        if(username == null || username.isEmpty())
            return false;

        UserEntity foundUser = userRepository.find("username=?1", username).firstResult();

        if(foundUser != null && foundUser.getStatus().equals("INACTIVE") && isAccountExpired(foundUser))
            return userRepository.deleteById(foundUser.getId());

        return foundUser == null;
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
        /*Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 10);

        long expInMilliseconds = calendar.getTimeInMillis();*/

        String username = user.getUsername();
        String role = user.getRole();
        try {
            return Cryptographer.encode(String.format("%s|%s|", username, role));
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
        if(userData == null || userData.size() != 2)
            return false;

        /*try {
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
        }*/

        String username = userData.get(0);

        UserEntity currentUser = userRepository.find("username=?1", username).firstResult();

        if(currentUser == null || currentUser.getStatus().equals("ACTIVE") || currentUser.getConfirmDateTime() == null)
            return false;

        return !isAccountExpired(currentUser);
    }

    private boolean isAccountExpired(UserEntity account) {
        LocalDateTime start = account.getConfirmDateTime();

        if(start == null)
            return false;

        LocalDateTime end = LocalDateTime.now();

        Instant startInstant = start.atZone(ZoneId.systemDefault()).toInstant();
        Instant endInstant = end.atZone(ZoneId.systemDefault()).toInstant();

        return endInstant.toEpochMilli() - startInstant.toEpochMilli() >= EXP_IN_MILLIS;
    }
}

// CASE 1 : User signs up => code is sent => he activates it on time
// CASE 2 : User signs up => code is sent => he misses the deadline
// CASE 3 : User signs up => code is sent => he never activates the code => record should be deleted