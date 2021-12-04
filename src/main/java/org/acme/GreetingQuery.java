package org.acme;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/hello")
public class GreetingQuery {

    @Inject
    @RestClient
    MessageRepository messageRepository;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello RESTEasy";
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{messageKey}")
    public String hello(@PathParam String messageKey) {
        System.out.println("messageKey=" + messageKey);
        String message = messageRepository.getMessage(messageKey);
        System.out.println("message=" + message);
        return message;
    }
}