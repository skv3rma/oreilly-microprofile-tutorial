package org.acme;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Counted
@Path("/frontend")
public class FrontendResource {

    @Inject
    @RestClient
    StudentRestClient student;

    @Inject
    Principal principal;

    @Inject
    JsonWebToken token;

    @RolesAllowed("user")
    @GET
    @Path("/tokeninfo")
    @Produces(MediaType.TEXT_PLAIN)
    public String tokeninfo() {
        String string = "Principal: " + principal.getName();

        string += ",\n";

        string += token.getClaimNames().stream().map(tok -> "\n " + tok + ": " + token.getClaim(tok))
                .collect(Collectors.toList()).toString();
        return string;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return student.hello();
    }

    @RolesAllowed("admin")
    @Timed(absolute = true, name = "listStudentsTime", displayName = "FrontendResource.listStudents()")
    @Retry(maxRetries = 4, delay = 1000)
    // @Timeout
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5, delay = 10000, successThreshold = 2)
    @Fallback(fallbackMethod = "listStudentsFallback")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/list")
    public List<String> listStudents() {
        return student.listStudents();
    }

    public List<String> listStudentsFallback() {
        // Return top students across all classes
        return Arrays.asList("Smart Sam", "Genius Gabby", "A-Student Angie", "Intelligent Irene");
    }
}