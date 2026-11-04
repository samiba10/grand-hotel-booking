package com.grandhotel.booking;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** System settings (ADMIN) and per-request currency. Restores the defaults after each test. */
class SettingsApiTest extends ApiTestSupport {

  private static final Map<String, Object> DEFAULTS =
      Map.of(
          "defaultCurrency", "USD",
          "supportedCurrencies", List.of("USD", "ILS"),
          "regionCurrencies", Map.of("IL", "ILS", "US", "USD"));

  @AfterEach
  void restoreDefaults() throws Exception {
    send(put("/api/settings"), admin(), DEFAULTS).andExpect(status().isOk());
  }

  @Test
  void anySignedInUserReadsSettings() throws Exception {
    mvc.perform(get("/api/settings").header(AUTH, guest1()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.defaultCurrency").value("USD"))
        .andExpect(jsonPath("$.supportedCurrencies", hasItem("ILS")))
        .andExpect(jsonPath("$.regionCurrencies.IL").value("ILS"));
  }

  @Test
  void onlyAdminChangesSettings() throws Exception {
    var onlyUsd = Map.of("defaultCurrency", "USD", "supportedCurrencies", List.of("USD"), "regionCurrencies", Map.of());
    send(put("/api/settings"), guest1(), onlyUsd).andExpect(status().isForbidden());
    send(put("/api/settings"), staff(), onlyUsd).andExpect(status().isForbidden());
    send(put("/api/settings"), admin(), onlyUsd)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.supportedCurrencies.length()").value(1))
        .andExpect(jsonPath("$.updatedBy").value("admin"));

    // the change is audited with before -> after
    mvc.perform(get("/api/audit").param("entityType", "SystemSettings").param("entityId", "1").header(AUTH, admin()))
        .andExpect(jsonPath("$[0].action").value("SETTINGS_UPDATED"));
  }

  @Test
  void inconsistentSettingsAreRejected() throws Exception {
    send(put("/api/settings"), admin(),
            Map.of("defaultCurrency", "ILS", "supportedCurrencies", List.of("USD"), "regionCurrencies", Map.of()))
        .andExpect(status().isBadRequest());
    send(put("/api/settings"), admin(),
            Map.of("defaultCurrency", "USD", "supportedCurrencies", List.of("USD", "EUR"), "regionCurrencies", Map.of()))
        .andExpect(status().isBadRequest()); // pricing cannot quote EUR
  }

  @Test
  void availabilityAndBookingUseTheRequestedCurrency() throws Exception {
    LocalDate in = LocalDate.now().plusDays(400);
    mvc.perform(
            get("/api/rooms/availability")
                .param("checkin", in.toString())
                .param("checkout", in.plusDays(1).toString())
                .param("currency", "ILS")
                .header(AUTH, guest1()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currency").value("ILS"));

    send(post("/api/bookings"), guest1(),
            Map.of("roomId", 3, "checkIn", in.toString(), "checkOut", in.plusDays(1).toString(), "currency", "ILS"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.currency").value("ILS"));
  }

  @Test
  void currencyNotOfferedIsRejected() throws Exception {
    LocalDate in = LocalDate.now().plusDays(410);
    mvc.perform(
            get("/api/rooms/availability")
                .param("checkin", in.toString())
                .param("checkout", in.plusDays(1).toString())
                .param("currency", "EUR")
                .header(AUTH, guest1()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Currency EUR is not offered"));
  }
}
