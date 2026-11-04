package com.grandhotel.booking.settings;

import com.grandhotel.booking.BookingProperties;
import com.grandhotel.booking.audit.AuditService;
import com.grandhotel.booking.settings.SettingsDtos.SettingsResponse;
import com.grandhotel.booking.settings.SettingsDtos.UpdateSettingsRequest;
import com.grandhotel.common.exception.BadRequestException;
import com.grandhotel.common.security.AuthenticatedUser;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runtime system configuration. Currency rules: the admin offers a subset of what pricing can
 * quote, picks the default, and maps regions to currencies. The server is the source of truth: a
 * currency that is not offered is rejected even if a client sends it.
 */
@Service
public class SettingsService {

  private final SystemSettingsRepository repo;
  private final BookingProperties props;
  private final AuditService audit;
  private final Clock clock;

  public SettingsService(
      SystemSettingsRepository repo, BookingProperties props, AuditService audit, Clock clock) {
    this.repo = repo;
    this.props = props;
    this.audit = audit;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public SettingsResponse get() {
    return toResponse(load());
  }

  @Transactional
  public SettingsResponse update(UpdateSettingsRequest req, AuthenticatedUser user) {
    List<String> supported = req.supportedCurrencies().stream().distinct().toList();
    for (String c : supported) {
      if (!props.priceableCurrencies().contains(c)) {
        throw new BadRequestException("Currency " + c + " cannot be priced (supported by pricing: " + props.priceableCurrencies() + ")");
      }
    }
    if (!supported.contains(req.defaultCurrency())) {
      throw new BadRequestException("Default currency must be one of the supported currencies");
    }
    req.regionCurrencies()
        .forEach(
            (region, currency) -> {
              if (!supported.contains(currency)) {
                throw new BadRequestException("Region " + region + " maps to unsupported currency " + currency);
              }
            });

    SystemSettings settings = load();
    String before = settings.describe();
    settings.update(
        req.defaultCurrency(),
        supported,
        new LinkedHashMap<>(req.regionCurrencies()),
        user.username(),
        clock.instant());
    audit.record("SETTINGS_UPDATED", "SystemSettings", SystemSettings.ID, before + " -> " + settings.describe());
    return toResponse(settings);
  }

  /** Requested currency if offered, the default when none was asked for, 400 otherwise. */
  @Transactional(readOnly = true)
  public String resolveCurrency(String requested) {
    SystemSettings settings = load();
    if (requested == null || requested.isBlank()) {
      return settings.getDefaultCurrency();
    }
    String currency = requested.trim().toUpperCase(java.util.Locale.ROOT);
    if (!settings.supportedCurrencies().contains(currency)) {
      throw new BadRequestException("Currency " + currency + " is not offered");
    }
    return currency;
  }

  private SystemSettings load() {
    return repo.findById(SystemSettings.ID)
        .orElseThrow(() -> new IllegalStateException("system_settings row missing"));
  }

  private SettingsResponse toResponse(SystemSettings s) {
    return new SettingsResponse(
        s.getDefaultCurrency(),
        s.supportedCurrencies(),
        s.regionCurrencies(),
        props.priceableCurrencies(),
        s.getUpdatedBy(),
        s.getUpdatedAt());
  }
}
