package fr.btn.resources;

import fr.btn.dtos.MailClient;
import fr.btn.entities.UserEntity;
import fr.btn.repositories.UserRepository;
import fr.btn.securityUtils.Argon2;
import fr.btn.securityUtils.TokenUtil;
import fr.btn.services.MailService;
import fr.btn.utils.Utils;
import fr.btn.utils.Validator;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name="Authentication")
public class AuthResource {
    @Context
    UriInfo request;
    private static final String API_KEY = ConfigProvider.getConfig().getValue("app-config.api-key", String.class);

    @Inject
    @RestClient
    MailService mailService;

    @Inject
    UserRepository userRepository;

    @POST
    @Path("/register")
    @PermitAll
    @Transactional
    @Produces(MediaType.TEXT_PLAIN)
    public Response register(@FormParam("email") String email, @FormParam("username") String username, @FormParam("password") String password) {
        // email validation
        Response emailValidationRes = isEmailValid(email);
        if(emailValidationRes.getStatus() != 200)
            return emailValidationRes;

        // username validation
        if(!isUsernameValid(username))
            return Response.ok("Invalid Username.").status(Response.Status.BAD_REQUEST).build();

        // password validation
        if(!Validator.validatePassword(password))
            return Response.ok("Invalid password.").status(Response.Status.BAD_REQUEST).build();

        String hashedPassword = Argon2.getHashedPassword(password);

        List<String> userData = Arrays.asList(username, hashedPassword, email);
        String encodedActivationStr = Utils.generateEncodedStringWithUserData(userData);

        URI uri = UriBuilder
                .fromUri(request.getBaseUri())
                .path("auth/account_confirm")
                .queryParam("code", encodedActivationStr).build();


        boolean isSent = sendMail(email, "Account Activation", uri.toString());

        if(!isSent)
            return Response.ok("An error has occurred. Please try again later.").status(Response.Status.INTERNAL_SERVER_ERROR).build();

        return Response.ok("Please activate your account by clicking on the link that has been sent to your email.").status(Response.Status.CREATED).build();
    }

    @POST
    @Path("/login")
    @Transactional
    @Produces(MediaType.TEXT_PLAIN)
    public Response login(@FormParam("username") String username, @FormParam("password") String password) {
        if(username == null || username.isEmpty())
            return Response.ok("Username is required.").status(Response.Status.BAD_REQUEST).build();

        if(!Validator.validatePassword(password))
            return Response.ok("Password is required.").status(Response.Status.BAD_REQUEST).build();

        // send link to forgotUsername/register here
        UserEntity foundUser = userRepository.findUserByUsername(username);
        if(foundUser == null)
            return Response.ok("User does not exist.").status(Response.Status.NOT_FOUND).build();

        Response accessValidatorRes = Validator.validateAccess(foundUser);
        if(accessValidatorRes.getStatus() != 200)
            return accessValidatorRes;

        // send link to forgotPassword here
        if(!Argon2.validate(password, foundUser.getPassword())) {
            Validator.evaluateAccessAndFailedAttempts(foundUser);

            return Response.ok("Incorrect Password.").status(Response.Status.NOT_ACCEPTABLE).build();
        }

        int codePin = generateUniquePinCode();

        sendMail(foundUser.getEmail(), "Access Pin Code", Integer.toString(codePin));

        foundUser.setNumFailAttempts(0);
        foundUser.setLastAccess(LocalDateTime.now());
        foundUser.setPinCode(codePin);

        return Response.ok("Please check your email and confirm your login.").build();
    }

    @GET
    @Transactional
    @Path("/account_confirm")
    @Produces(MediaType.TEXT_PLAIN)
    public Response activateAccount(@QueryParam("code") String encodedData) {
        try {
            List<String> userData = Utils.decodeAndExtractData(encodedData);

            if(userData == null || userData.size() != 4)
                return Response.ok("Invalid code").status(Response.Status.NOT_ACCEPTABLE).build();

            String username = userData.get(0);
            String password = userData.get(1);
            String email = userData.get(2);
            long createdTime = Long.parseLong(userData.get(3));

            if(userRepository.countByUsername(username) > 0)
                return Response.ok("This account is expired.").status(Response.Status.NOT_ACCEPTABLE).build();

            if(Utils.isCodeExpired(createdTime))
                return Response.ok("This code is expired.").status(Response.Status.NOT_ACCEPTABLE).build();


            UserEntity userEntity = UserEntity
                    .builder()
                    .username(username)
                    .password(password)
                    .email(email)
                    .role("USER")
                    .status("ACTIVE")
                    .build();

            userRepository.persist(userEntity);

            return Response.ok("Account has been successfully activated.").build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok("An error has occurred.").status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Transactional
    @Path("/login_confirm")
    @Produces(MediaType.TEXT_PLAIN)
    public Response confirmLogin(@FormParam("username") String username, @FormParam("password") String password, @FormParam("pin_code") Integer pinCode) {
        if(username == null || password == null || pinCode == null)
            return Response.ok("Invalid data.").status(Response.Status.BAD_REQUEST).build();

        UserEntity foundUser = userRepository.findUserByUsername(username);
        if(foundUser == null)
            return Response.ok("User does not exist.").status(Response.Status.BAD_REQUEST).build();

        Response accessValidatorRes = Validator.validateAccess(foundUser);
        if(accessValidatorRes.getStatus() != 200)
            return accessValidatorRes;

        if(!Argon2.validate(password, foundUser.getPassword())) {
            Validator.evaluateAccessAndFailedAttempts(foundUser);

            return Response.ok("Incorrect Password.").status(Response.Status.NOT_ACCEPTABLE).build();
        }

        if(foundUser.getPinCode() == null)
            return Response.ok("Code is expired.").status(Response.Status.NOT_ACCEPTABLE).build();

        Instant lastAccessInstant = foundUser.getLastAccess().atZone(ZoneId.systemDefault()).toInstant();
        if(Objects.equals(foundUser.getPinCode(), pinCode)) {
            if(Utils.isCodeExpired(lastAccessInstant.toEpochMilli()))
                return Response.ok("Code is expired.").status(Response.Status.NOT_ACCEPTABLE).build();
        } else {
            Validator.evaluateAccessAndFailedAttempts(foundUser);

            return Response.ok("Incorrect Pin Code.").status(Response.Status.NOT_ACCEPTABLE).build();
        }

        foundUser.setPinCode(null);

        foundUser.setNumFailAttempts(0);
        foundUser.setLastAccess(LocalDateTime.now());

        String token = TokenUtil.generateJwt(username, foundUser.getRole());

        return Response.ok("Access Granted.").header(HttpHeaders.AUTHORIZATION, token).build();
    }


    @PUT
    @Transactional
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/confirm_reset_password")
    public Response confirmResetPassword(@QueryParam("code") String encodedData) {
        try {
            List<String> userData = Utils.decodeAndExtractData(encodedData);

            if(!Validator.validateUsername(userData.get(0)))
                return Response.ok("Invalid code.").status(Response.Status.NOT_ACCEPTABLE).build();

            UserEntity existingUser = userRepository.findUserByUsername(userData.get(0));
            if(existingUser == null)
                return Response.ok("User does not exist.").status(Response.Status.NOT_ACCEPTABLE).build();

            if(Utils.isCodeExpired(Long.parseLong(userData.get(userData.size() - 1))))
                return Response.ok("This code is expired.").status(Response.Status.NOT_ACCEPTABLE).build();

            Response canAccessRes = Validator.validateAccess(existingUser);
            if(canAccessRes.getStatus() != 200)
                return canAccessRes;

            existingUser.setPassword(userData.get(1));

            return Response.ok("Password has been reset successfully.").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok("Impossible to reset password.").status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    private Response isEmailValid(String email) {
        if(email == null || email.isEmpty())
            return Response.ok("Email is required.").status(Response.Status.BAD_REQUEST).build();

        if(!Validator.validateEmail(email))
            return Response.ok("Invalid Email.").status(Response.Status.BAD_REQUEST).build();

        UserEntity foundUser = userRepository.findUserByEmail(email);

        if(foundUser != null)
            return Response.ok("This email is already registered.").status(Response.Status.BAD_REQUEST).build();

        return Response.ok().build();
    }
    private boolean isUsernameValid(String username) {
        if(username == null || username.isEmpty())
            return false;

        if(!Validator.validateUsername(username))
            return false;

        long count = userRepository.countByUsername(username);

        return count == 0;
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

    private int generateUniquePinCode() {
        int pinCode;

        do {
            pinCode = Utils.generateCodePin(4);
        } while(userRepository.count("pinCode=?1", pinCode) != 0);

        return pinCode;
    }
}

//Account activation
    // CASE 1 : User signs up => code is sent => he activates it on time
    // CASE 2 : User signs up => code is sent => he misses the deadline
//Login
    // CASE 1 : User enters correct password => code pin sent => he enters the code pin within the time limit.
    // CASE 2 : User enters correct password => code pin sent => he enters the code pin too late.
    // CASE 3 : User enters incorrect password => augment counter
