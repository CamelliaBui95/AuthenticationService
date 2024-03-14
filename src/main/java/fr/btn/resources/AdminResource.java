package fr.btn.resources;

import fr.btn.dtos.ModifyAccountForm;
import fr.btn.dtos.UserDto;
import fr.btn.entities.UserEntity;
import fr.btn.repositories.UserRepository;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name="Admin")
public class AdminResource {
    @Inject
    UserRepository userRepository;

    private Set<String> statusConstraints;

    private Set<String> roleConstraints;

    public AdminResource() {
        this.statusConstraints = new HashSet<>();

        statusConstraints.add("ACTIVE");
        statusConstraints.add("INACTIVE");

        this.roleConstraints = new HashSet<>();

        roleConstraints.add("USER");
        roleConstraints.add("ADMIN");
    }

    @GET
    @Path("/all_users")
    @RolesAllowed("ADMIN")
    public Response getAll() {
        List<UserEntity> userEntities = userRepository.listAll();

        return Response.ok(UserDto.toDtoList(userEntities)).build();
    }

    @PUT
    @Path("/modify_user_account")
    @Transactional
    @RolesAllowed("ADMIN")
    @Produces(MediaType.TEXT_PLAIN)
    public Response modifyAccount(ModifyAccountForm form) {
        if(form == null || form.getUserId() == null)
            return Response.status(Response.Status.BAD_REQUEST).build();

        UserEntity existingUser = userRepository.findById(form.getUserId());
        if(existingUser == null)
            return Response.status(Response.Status.NOT_FOUND).build();

        if(form.getStatus() != null) {
            if(!statusConstraints.contains(form.getStatus()))
                return Response.ok("Status must be either INACTIVE or ACTIVE").status(Response.Status.NOT_ACCEPTABLE).build();
            else existingUser.setStatus(form.getStatus());
        }

        if(form.getRole() != null) {
            if(!roleConstraints.contains(form.getRole()))
                return Response.ok("Status must be either USER or ADMIN").status(Response.Status.NOT_ACCEPTABLE).build();
            else existingUser.setRole(form.getRole());
        }

        return Response.ok().build();
    }


}
