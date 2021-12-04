package org.acme;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@Slf4j
@QuarkusTest
@QuarkusTestResource(MockServer.class)
public class GreetingQueryTest {

    @Test
    public void testHelloEndpoint() {
        given()
          .get("hello")
          .then()
             .statusCode(200)
             .body(is("Hello RESTEasy"));
    }

    @Test
    public void testHelloEndpointWithKey() {
        log.info("Test started");
        MockServer.getMockServerClient().when(
                HttpRequest.request("/message-repository/testKey")
                        .withMethod("GET"))
                .respond(HttpResponse.response()
                        .withStatusCode(200).withBody("Test-message"));

        log.info("Mock initialized");
        try {
            given()
                    .when().get("/hello/testKey")
                    .then()
                    .statusCode(200)
                    .body(is("Test-message"));
        }
        catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
        log.info("Test done");
    }
}