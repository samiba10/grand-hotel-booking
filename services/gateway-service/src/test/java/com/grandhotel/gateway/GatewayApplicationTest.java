package com.grandhotel.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayApplicationTest {

  @Autowired Environment env;
  WebTestClient client;

  @BeforeEach
  void setUp() {
    client =
        WebTestClient.bindToServer()
            .baseUrl("http://localhost:" + env.getRequiredProperty("local.server.port"))
            .build();
  }

  @Test
  void protectedRouteWithoutTokenIs401WithTransactionId() {
    client
        .get()
        .uri("/api/bookings/1")
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectHeader()
        .exists("X-Transaction-Id")
        .expectBody()
        .jsonPath("$.transactionId")
        .exists();
  }

  @Test
  void corsPreflightFromAllowedOriginIsAccepted() {
    client
        .options()
        .uri("/api/bookings/1")
        .header("Origin", "http://localhost:5173")
        .header("Access-Control-Request-Method", "GET")
        .exchange()
        .expectHeader()
        .valueEquals("Access-Control-Allow-Origin", "http://localhost:5173");
  }

  @Test
  void rateLimitHeaderIsReturned() {
    client.get().uri("/api/bookings/1").exchange().expectHeader().exists("X-RateLimit-Remaining");
  }
}
