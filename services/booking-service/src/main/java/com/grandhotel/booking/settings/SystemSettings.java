package com.grandhotel.booking.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Single-row system configuration, edited by ADMIN at runtime (no redeploy). */
@Entity
@Table(name = "system_settings")
public class SystemSettings {

  static final long ID = 1L;

  @Id private Long id;

  @Column(name = "default_currency", nullable = false, length = 3)
  private String defaultCurrency;

  @Column(name = "supported_currencies", nullable = false, length = 100)
  private String supportedCurrencies;

  @Column(name = "region_currencies", nullable = false, length = 500)
  private String regionCurrencies;

  @Column(name = "updated_by", nullable = false, length = 50)
  private String updatedBy;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected SystemSettings() {}

  public String getDefaultCurrency() {
    return defaultCurrency;
  }

  public List<String> supportedCurrencies() {
    return Arrays.stream(supportedCurrencies.split(",")).map(String::trim).filter(c -> !c.isEmpty()).toList();
  }

  /** Region (ISO-3166 alpha-2) -> currency, in stored order. */
  public Map<String, String> regionCurrencies() {
    Map<String, String> map = new LinkedHashMap<>();
    for (String pair : regionCurrencies.split(",")) {
      String[] kv = pair.split("=");
      if (kv.length == 2) {
        map.put(kv[0].trim(), kv[1].trim());
      }
    }
    return map;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  void update(
      String defaultCurrency,
      List<String> supported,
      Map<String, String> regions,
      String user,
      Instant now) {
    this.defaultCurrency = defaultCurrency;
    this.supportedCurrencies = String.join(",", supported);
    this.regionCurrencies =
        String.join(",", regions.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toList());
    this.updatedBy = user;
    this.updatedAt = now;
  }

  String describe() {
    return "default=" + defaultCurrency + " supported=" + supportedCurrencies + " regions=" + regionCurrencies;
  }
}
