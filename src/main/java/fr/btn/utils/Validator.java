package fr.btn.utils;

import fr.btn.entities.UserEntity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

public class Validator {
    public static Response validateAccess(UserEntity user) {
        if(user.getNumFailAttempts() == null)
            return Response.ok().build();

        LocalDateTime lastAccess = user.getLastAccess();
        int nbFails = user.getNumFailAttempts();
        int nbAttemptsMax = 3;

        if(nbFails < nbAttemptsMax)
            return Response.ok().build();

        int lockedMinutes = (nbFails - nbAttemptsMax + 1) * 10;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lockedUntil = lastAccess.plusMinutes(lockedMinutes);

        boolean canAccess = now.isAfter(lockedUntil);
        if(canAccess)
            return Response.ok().build();

        return Response.ok("Your account is locked until " + lockedUntil, MediaType.TEXT_PLAIN).status(Response.Status.FORBIDDEN).build();
    }

    public static boolean validateUsername(String username) {
        if(username == null || username.isEmpty())
            return false;

        String pattern = "^[a-zA-Z0-9]([._-](?![._-])|[a-zA-Z0-9]){3,18}[a-zA-Z0-9]$";

        return Pattern.compile(pattern).matcher(username).matches();
    }

    public static boolean validatePassword(String password) {
        return password != null && password.length() >= 8;
    }

    public static boolean validateEmail(String email) {
        if(email == null || email.isEmpty())
            return false;

        String pattern = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@"
                + "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";

        return Pattern.compile(pattern).matcher(email).matches();
    }

    public static void evaluateAccessAndFailedAttempts(UserEntity user) {
        int numFails = user.getNumFailAttempts() == null ? 0 : user.getNumFailAttempts();
        user.setNumFailAttempts(numFails + 1);

        if(user.getNumFailAttempts() >= 3)
            user.setLastAccess(LocalDateTime.now());
    }
}
