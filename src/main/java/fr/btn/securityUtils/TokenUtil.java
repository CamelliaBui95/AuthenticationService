package fr.btn.securityUtils;

import io.smallrye.jwt.build.Jwt;
import io.vertx.ext.auth.impl.jose.JWT;
import jakarta.inject.Singleton;

import java.util.Calendar;

public class TokenUtil {
    //private static final String SECRET = "MySecret";

    public static String generateJwt(String username, String role) {

        return Jwt.issuer("authentication-jwt-service")
                .subject(username)
                .groups(role)
                .expiresIn(60 * 5)
                .sign();
    }

}
// @RolesAllowed({ "User", "Admin" })
