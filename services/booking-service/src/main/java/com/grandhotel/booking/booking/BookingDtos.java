package com.grandhotel.booking.booking;

import com.grandhotel.booking.room.RoomType;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;

public final class BookingDtos {

  private BookingDtos() {}

  /**
   * @param guestId optional for guests (defaults to their own profile), required for staff
   * @param currency optional; must be offered by system settings, default currency otherwise
   */
  public record CreateBookingRequest(
      @NotNull Long roomId,
      @NotNull LocalDate checkIn,
      @NotNull LocalDate checkOut,
      Long guestId,
      String currency) {}

  public record UpdateBookingRequest(
      @NotNull Long roomId, @NotNull LocalDate checkIn, @NotNull LocalDate checkOut) {}

  public record BookingResponse(
      Long id,
      Long guestId,
      String guestName,
      Long roomId,
      String roomNumber,
      RoomType roomType,
      LocalDate checkIn,
      LocalDate checkOut,
      long nights,
      BookingStatus status,
      long totalAmountMinor,
      String currency,
      long fxRateMicros,
      Instant createdAt,
      Instant updatedAt) {

    public static BookingResponse from(Booking b) {
      return new BookingResponse(
          b.getId(),
          b.getGuest().getId(),
          b.getGuest().fullName(),
          b.getRoom().getId(),
          b.getRoom().getRoomNumber(),
          b.getRoom().getRoomType(),
          b.getCheckIn(),
          b.getCheckOut(),
          new StayDates(b.getCheckIn(), b.getCheckOut()).nights(),
          b.getStatus(),
          b.getTotal().amountMinor(),
          b.getTotal().currency(),
          b.getFxRateMicros(),
          b.getCreatedAt(),
          b.getUpdatedAt());
    }
  }
}
