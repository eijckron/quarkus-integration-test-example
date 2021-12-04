package org.acme;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@RegisterRestClient(configKey="message-repository")
public interface MessageRepository {

    @GET
    @Path("/{messageKey}")
    @Produces("text/plain")
    String getMessage(@PathParam String messageKey);
}
