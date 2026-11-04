package com.grandhotel.booking.booking;

import com.grandhotel.booking.guest.Guest;
import com.grandhotel.booking.room.Room;
import com.grandhotel.common.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "bookings")
public class Booking {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "guest_id")
  private Guest guest;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "room_id")
  private Room room;

  @Column(name = "check_in", nullable = false)
  private LocalDate checkIn;

  @Column(name = "check_out", nullable = false)
  private LocalDate checkOut;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private BookingStatus status;

  /** Price locked at booking time, BIGINT minor units. */
  @Column(name = "total_amount_minor", nullable = false)
  private long totalAmountMinor;

  @Column(nullable = false, length = 3)
  private String currency;

  /** FX rate used for the price - the amount can always be explained later. */
  @Column(name = "fx_rate_micros", nullable = false)
  private long fxRateMicros;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Booking() {}

  public Booking(
      Guest guest, Room room, StayDates dates, Money total, long fxRateMicros, Instant now) {
    this.guest = guest;
    this.status = BookingStatus.CONFIRMED;
    this.createdAt = now;
    apply(room, dates, total, fxRateMicros, now);
  }

  public void reschedule(Room room, StayDates dates, Money total, long fxRateMicros, Instant now) {
    apply(room, dates, total, fxRateMicros, now);
  }

  /** Soft cancel: the row (and its history) is kept, the room is freed. */
  public void cancel(Instant now) {
    this.status = BookingStatus.CANCELLED;
    this.updatedAt = now;
  }

  private void apply(Room room, StayDates dates, Money total, long fxRateMicros, Instant now) {
    this.room = room;
    this.checkIn = dates.checkIn();
    this.checkOut = dates.checkOut();
    this.totalAmountMinor = total.amountMinor();
    this.currency = total.currency();
    this.fxRateMicros = fxRateMicros;
    this.updatedAt = now;
  }

  public boolean isActive() {
    return status == BookingStatus.CONFIRMED;
  }

  public Long getId() {
    return id;
  }

  public Guest getGuest() {
    return guest;
  }

  public Room getRoom() {
    return room;
  }

  public LocalDate getCheckIn() {
    return checkIn;
  }

  public LocalDate getCheckOut() {
    return checkOut;
  }

  public BookingStatus getStatus() {
    return status;
  }

  public Money getTotal() {
    return Money.of(totalAmountMinor, currency);
  }

  public long getFxRateMicros() {
    return fxRateMicros;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
