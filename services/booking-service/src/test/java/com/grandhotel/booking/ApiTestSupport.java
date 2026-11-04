package com.grandhotel.booking;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import com.grandhotel.booking.booking.StayDates;
import com.grandhotel.booking.pricing.PricingClient;
import com.grandhotel.booking.pricing.PricingQuote;
import com.grandhotel.booking.room.RoomType;
import com.grandhotel.common.security.JwtService;
import com.grandhotel.common.security.Role;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Set;
import com.grandhotel.common.context.AuditContextFilter;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

/** MockMvc through the REAL security chain + helpers to act as the demo users. */
@SpringBootTest
public abstract class ApiTestSupport {

  public static final String AUTH = "Authorization";

  /** Mock price: 100.00 USD per night for every room type. */
  public static final long NIGHTLY = 10_000;

  public static final long FX = 270_270;

  @Autowired protected WebApplicationContext context;
  @Autowired protected JsonMapper json;
  @Autowired protected JwtService jwt;
  @MockitoBean protected PricingClient pricing;
  protected MockMvc mvc;

  @BeforeEach
  protected void setUpMvc() {
    // Same filter order as production: request context (tx/session ids) first, then security.
    mvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(context.getBean(AuditContextFilter.class))
            .apply(springSecurity())
            .build();
    when(pricing.quote(any(), anyInt(), any(), anyString()))
        .thenAnswer(
            inv -> {
              StayDates dates = inv.getArgument(0);
              Set<RoomType> types = inv.getArgument(2);
              String currency = inv.getArgument(3);
              var totals = new EnumMap<RoomType, Long>(RoomType.class);
              types.forEach(t -> totals.put(t, NIGHTLY * dates.nights()));
              return new PricingQuote(currency, totals, FX, Instant.now(), false);
            });
  }

  protected String guest1() {
    return bearer("guest1", Role.GUEST, 1L);
  }

  protected String guest2() {
    return bearer("guest2", Role.GUEST, 2L);
  }

  protected String staff() {
    return bearer("staff", Role.STAFF, null);
  }

  protected String admin() {
    return bearer("admin", Role.ADMIN, null);
  }

  protected String bearer(String user, Role role, Long guestId) {
    return "Bearer " + jwt.issue(user, Set.of(role), guestId).token();
  }

  protected ResultActions send(MockHttpServletRequestBuilder req, String bearer, Object body)
      throws Exception {
    return mvc.perform(
        req.header(AUTH, bearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(body)));
  }

  protected static long idOf(ResultActions result) throws Exception {
    String body = result.andReturn().getResponse().getContentAsString();
    return ((Number) JsonPath.read(body, "$.id")).longValue();
  }
}
