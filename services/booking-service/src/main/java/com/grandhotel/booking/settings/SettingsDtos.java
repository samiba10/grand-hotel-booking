package com.grandhotel.booking.settings;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class SettingsDtos {

  private SettingsDtos() {}

  public record SettingsResponse(
      String defaultCurrency,
      List<String> supportedCurrencies,
      Map<String, String> regionCurrencies,
      List<String> priceableCurrencies,
      String updatedBy,
      Instant updatedAt) {}

  public record UpdateSettingsRequest(
      @NotNull @Pattern(regexp = "^[A-Z]{3}$") String defaultCurrency,
      @NotEmpty List<@Pattern(regexp = "^[A-Z]{3}$") String> supportedCurrencies,
      @NotNull Map<@Pattern(regexp = "^[A-Z]{2}$") String, @Pattern(regexp = "^[A-Z]{3}$") String>
          regionCurrencies) {}
}
