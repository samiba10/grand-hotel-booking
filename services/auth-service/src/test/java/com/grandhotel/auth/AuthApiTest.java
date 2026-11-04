package com.grandhotel.auth;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import com.grandhotel.common.context.AuditContextFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
class AuthApiTest {

  @Autowired WebApplicationContext context;
  @Autowired JsonMapper json;
  MockMvc mvc;

  @BeforeEach
  void setUp() {
    // Same filter order as production: request context (tx/session ids) first, then security.
    mvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean(AuditContextFilter.class))
            .apply(springSecurity())
            .build();
  }

  @Test
  void guestLoginReturnsTokenGuestIdAndOwnScopedPermissions() throws Exception {
    login("guest1", "Guest123!")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token", notNullValue()))
        .andExpect(jsonPath("$.user.guestId").value(1))
        .andExpect(jsonPath("$.user.region").value("IL"))
        .andExpect(jsonPath("$.user.permissions", hasItem("booking:read:own")))
        .andExpect(jsonPath("$.user.permissions", not(hasItem("audit:read"))));
  }

  @Test
  void meReturnsPermissionsResolvedFromTheToken() throws Exception {
    String body = login("admin", "Admin123!").andReturn().getResponse().getContentAsString();
    String token = JsonPath.read(body, "$.token");

    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("admin"))
        .andExpect(jsonPath("$.permissions", hasItem("audit:read")));
  }

  @Test
  void wrongPasswordReturns401WithGenericMessage() throws Exception {
    login("guest1", "nope")
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid username or password"));
    login("nobody", "nope").andExpect(status().isUnauthorized());
  }

  @Test
  void meWithoutTokenReturns401() throws Exception {
    mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void everyResponseCarriesTransactionAndSessionIds() throws Exception {
    mvc.perform(get("/api/auth/me"))
        .andExpect(header().exists("X-Transaction-Id"))
        .andExpect(header().exists("X-Session-Id"))
        .andExpect(jsonPath("$.transactionId", notNullValue()));
  }

  @Test
  void clientSessionIdIsEchoedBack() throws Exception {
    mvc.perform(get("/api/auth/me").header("X-Session-Id", "session-1234-abcd"))
        .andExpect(header().string("X-Session-Id", "session-1234-abcd"));
  }

  private ResultActions login(String username, String password) throws Exception {
    return mvc.perform(
        post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username", username, "password", password))));
  }
}
