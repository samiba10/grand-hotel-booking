package com.grandhotel.booking.booking;

import com.grandhotel.common.exception.BadRequestException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Validated stay [checkIn, checkOut) - the check-out day is not a night. ONE definition of the date
 * rules, used by availability, create and update.
 */
public record StayDates(LocalDate checkIn, LocalDate checkOut) {

  public static final int MAX_NIGHTS = 30;

  public static StayDates of(LocalDate checkIn, LocalDate checkOut, Clock clock) {
    if (checkIn == null || checkOut == null) {
      throw new BadRequestException("checkIn and checkOut are required");
    }
    if (checkIn.isBefore(LocalDate.now(clock))) {
      throw new BadRequestException("checkIn cannot be in the past");
    }
    if (!checkOut.isAfter(checkIn)) {
      throw new BadRequestException("checkOut must be after checkIn");
    }
    StayDates dates = new StayDates(checkIn, checkOut);
    if (dates.nights() > MAX_NIGHTS) {
      throw new BadRequestException("Maximum stay is " + MAX_NIGHTS + " nights");
    }
    return dates;
  }

  public long nights() {
    return ChronoUnit.DAYS.between(checkIn, checkOut);
  }
}
