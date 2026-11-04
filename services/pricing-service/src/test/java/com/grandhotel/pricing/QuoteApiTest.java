package com.grandhotel.pricing;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.grandhotel.common.exception.ServiceUnavailableException;
import com.grandhotel.common.security.JwtService;
import com.grandhotel.common.security.Role;
import com.grandhotel.pricing.fx.FxClient;
import com.grandhotel.pricing.fx.FxClient.FxQuote;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import com.grandhotel.common.context.AuditContextFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
class QuoteApiTest {

  @Autowired WebApplicationContext context;
  @Autowired JwtService jwt;
  @Autowired JsonMapper json;
  @MockitoBean FxClient fx;
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
  void quotesPerNightInUsdUsingTheLiveRate() throws Exception {
    when(fx.rate("ILS", "USD"))
        .thenReturn(new FxQuote("ILS", "USD", 270_270, Instant.now(), "test", false));

    // March = off season, 10% occupancy -> 370.00 ILS -> 100.00 USD per night
    quote(Map.of(
            "checkIn", "2027-03-01", "checkOut", "2027-03-03", "occupancyPercent", 10,
            "roomTypes", Set.of("STANDARD"), "currency", "USD"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quotes[0].totalMinor").value(20_000))
        .andExpect(jsonPath("$.quotes[0].nights.length()").value(2))
        .andExpect(jsonPath("$.fx.rateMicros").value(270_270))
        .andExpect(jsonPath("$.fx.stale").value(false));
  }

  @Test
  void fxOutageWithoutAnyKnownRateIs503() throws Exception {
    when(fx.rate(anyString(), anyString()))
        .thenThrow(new ServiceUnavailableException("Exchange rates are unavailable", null));

    quote(Map.of(
            "checkIn", "2027-03-01", "checkOut", "2027-03-02", "occupancyPercent", 0,
            "roomTypes", Set.of("SUITE"), "currency", "USD"))
        .andExpect(status().isServiceUnavailable());
  }

  @Test
  void invalidStayIs400() throws Exception {
    quote(Map.of(
            "checkIn", "2027-03-02", "checkOut", "2027-03-02", "occupancyPercent", 0,
            "roomTypes", Set.of("SUITE"), "currency", "USD"))
        .andExpect(status().isBadRequest());
  }

  private ResultActions quote(Map<String, Object> body) throws Exception {
    return mvc.perform(
        post("/api/pricing/quotes")
            .header("Authorization", "Bearer " + jwt.issue("g", Set.of(Role.GUEST), 1L).token())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(body)));
  }
}
