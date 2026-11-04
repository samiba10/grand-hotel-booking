package com.grandhotel.booking;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param hotelId the single hotel of the assignment (multi-property ready: rooms carry hotel_id)
 * @param currency currency bookings are charged in
 */
@ConfigurationProperties(prefix = "app.booking")
public record BookingProperties(long hotelId, String currency) {}
