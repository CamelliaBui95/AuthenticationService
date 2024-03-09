package fr.btn.services;

import fr.btn.dtos.MailClient;
import io.smallrye.common.annotation.Blocking;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "mail-service")
@Path("/mail")
public interface MailService {
    @POST
    @Blocking
    @Transactional
    Response send(@HeaderParam("x-api-key") String apiKey, MailClient mailClient);
}

