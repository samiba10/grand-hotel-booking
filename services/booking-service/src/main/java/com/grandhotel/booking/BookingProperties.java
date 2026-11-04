package com.grandhotel.booking;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param hotelId the single hotel of the assignment (multi-property ready: rooms carry hotel_id)
 * @param priceableCurrencies currencies the pricing + FX chain can quote; the admin may enable
 *     any subset of these in system settings
 */
@ConfigurationProperties(prefix = "app.booking")
public record BookingProperties(long hotelId, List<String> priceableCurrencies) {

  public BookingProperties {
    priceableCurrencies = priceableCurrencies == null ? List.of() : List.copyOf(priceableCurrencies);
  }
}
