package fr.btn.resources;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

@SecurityScheme(
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT"
)
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {
    @GET
    @Path("/all")
    @RolesAllowed("ADMIN")
    public void getUsers() {
        System.out.println("Accessed");
    }
}
