package com.grandhotel.fx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.grandhotel.common.security.JwtService;
import com.grandhotel.common.security.Role;
import com.grandhotel.fx.rate.FxService;
import java.util.Set;
import com.grandhotel.common.context.AuditContextFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class FxApiTest {

  @Autowired WebApplicationContext context;
  @Autowired JwtService jwt;
  @Autowired FxService fx;
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

  private String bearer() {
    return "Bearer " + jwt.issue("svc", Set.of(Role.GUEST), 1L).token();
  }

  @Test
  void requiresAuthentication() throws Exception {
    mvc.perform(get("/api/fx/rates/latest").param("base", "ILS").param("quote", "USD"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void returnsLatestRateAsIntegerMicros() throws Exception {
    mvc.perform(
            get("/api/fx/rates/latest")
                .param("base", "ILS")
                .param("quote", "USD")
                .header("Authorization", bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rateMicros").isNumber())
        .andExpect(jsonPath("$.source").exists());
  }

  @Test
  void sameCurrencyIsIdentity() throws Exception {
    mvc.perform(
            get("/api/fx/rates/latest")
                .param("base", "USD")
                .param("quote", "USD")
                .header("Authorization", bearer()))
        .andExpect(jsonPath("$.rateMicros").value(1_000_000));
  }

  @Test
  void unknownPairIs404() throws Exception {
    mvc.perform(
            get("/api/fx/rates/latest")
                .param("base", "ILS")
                .param("quote", "JPY")
                .header("Authorization", bearer()))
        .andExpect(status().isNotFound());
  }

  @Test
  void refreshStoresANewRateAndItsInverse() {
    long before = fx.latest("ILS", "USD").getRateMicros();

    fx.refresh();

    var after = fx.latest("ILS", "USD");
    assertThat(after.getSource()).isEqualTo("mock-market");
    assertThat(Math.abs(after.getRateMicros() - before)).isLessThanOrEqualTo(before * 30 / 10_000);
    long inverse = fx.latest("USD", "ILS").getRateMicros();
    assertThat(inverse * after.getRateMicros() / 1_000_000).isBetween(999_000L, 1_001_000L);
  }
}
