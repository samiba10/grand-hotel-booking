package com.grandhotel.booking.booking;

import com.grandhotel.booking.audit.AuditService;
import com.grandhotel.booking.booking.BookingDtos.BookingResponse;
import com.grandhotel.booking.booking.BookingDtos.CreateBookingRequest;
import com.grandhotel.booking.booking.BookingDtos.UpdateBookingRequest;
import com.grandhotel.booking.guest.Guest;
import com.grandhotel.booking.guest.GuestRepository;
import com.grandhotel.booking.pricing.PricingClient;
import com.grandhotel.booking.pricing.PricingQuote;
import com.grandhotel.booking.room.AvailabilityService;
import com.grandhotel.booking.room.Room;
import com.grandhotel.booking.room.RoomRepository;
import com.grandhotel.booking.settings.SettingsService;
import com.grandhotel.common.authz.AccessGuard;
import com.grandhotel.common.authz.Permissions;
import com.grandhotel.common.exception.BadRequestException;
import com.grandhotel.common.exception.ConflictException;
import com.grandhotel.common.exception.NotFoundException;
import com.grandhotel.common.money.Money;
import com.grandhotel.common.security.AuthenticatedUser;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Booking lifecycle. Owns the "no double booking" invariant and the fine-grained (ownership) AuthZ.
 * Every state change writes a business audit event in the same transaction.
 */
@Service
public class BookingService {

  private static final long NEW_BOOKING = -1L;

  private final BookingRepository bookings;
  private final RoomRepository rooms;
  private final GuestRepository guests;
  private final PricingClient pricing;
  private final AvailabilityService availability;
  private final AccessGuard guard;
  private final AuditService audit;
  private final SettingsService settings;
  private final Clock clock;

  public BookingService(
      BookingRepository bookings,
      RoomRepository rooms,
      GuestRepository guests,
      PricingClient pricing,
      AvailabilityService availability,
      AccessGuard guard,
      AuditService audit,
      SettingsService settings,
      Clock clock) {
    this.bookings = bookings;
    this.rooms = rooms;
    this.guests = guests;
    this.pricing = pricing;
    this.availability = availability;
    this.guard = guard;
    this.audit = audit;
    this.settings = settings;
    this.clock = clock;
  }

  @Transactional
  public BookingResponse create(CreateBookingRequest req, AuthenticatedUser user) {
    StayDates dates = StayDates.of(req.checkIn(), req.checkOut(), clock);
    Long guestId = req.guestId() != null ? req.guestId() : user.guestId();
    if (guestId == null) {
      throw new BadRequestException("guestId is required");
    }
    guard.require(user, Permissions.BOOKING_CREATE, guestId);
    Guest guest = guests.findById(guestId).orElseThrow(() -> new NotFoundException("Guest", guestId));

    String currency = settings.resolveCurrency(req.currency());
    Room room = bookableRoom(req.roomId());
    PricingQuote quote = quote(room, dates, currency); // remote call BEFORE taking the lock
    Money total = quote.totalFor(room.getRoomType());

    lockAndRequireFree(room, dates, NEW_BOOKING);
    Booking booking =
        bookings.save(new Booking(guest, room, dates, total, quote.fxRateMicros(), clock.instant()));
    audit.record(
        "BOOKING_CREATED",
        "Booking",
        booking.getId(),
        "room=" + room.getRoomNumber() + " " + dates.checkIn() + ".." + dates.checkOut()
            + " total=" + total.format());
    return BookingResponse.from(booking);
  }

  @Transactional(readOnly = true)
  public BookingResponse get(Long id, AuthenticatedUser user) {
    Booking booking = find(id);
    guard.require(user, Permissions.BOOKING_READ, booking.getGuest().getId());
    return BookingResponse.from(booking);
  }

  @Transactional
  public BookingResponse update(Long id, UpdateBookingRequest req, AuthenticatedUser user) {
    Booking booking = find(id);
    guard.require(user, Permissions.BOOKING_UPDATE, booking.getGuest().getId());
    if (!booking.isActive()) {
      throw new ConflictException("A cancelled booking cannot be changed");
    }
    StayDates dates = StayDates.of(req.checkIn(), req.checkOut(), clock);
    String before =
        "room=" + booking.getRoom().getRoomNumber() + " " + booking.getCheckIn() + ".."
            + booking.getCheckOut() + " total=" + booking.getTotal().format();

    Room room = bookableRoom(req.roomId());
    // A booking keeps the currency it was made in; changes are re-priced in that currency.
    PricingQuote quote = quote(room, dates, booking.getTotal().currency());
    Money total = quote.totalFor(room.getRoomType());

    lockAndRequireFree(room, dates, booking.getId());
    booking.reschedule(room, dates, total, quote.fxRateMicros(), clock.instant());
    audit.record(
        "BOOKING_UPDATED",
        "Booking",
        booking.getId(),
        before + " -> room=" + room.getRoomNumber() + " " + dates.checkIn() + ".."
            + dates.checkOut() + " total=" + total.format());
    return BookingResponse.from(booking);
  }

  /** DELETE = soft cancel: history and audit trail are preserved, the room is released. */
  @Transactional
  public void cancel(Long id, AuthenticatedUser user) {
    Booking booking = find(id);
    guard.require(user, Permissions.BOOKING_CANCEL, booking.getGuest().getId());
    if (!booking.isActive()) {
      throw new ConflictException("Booking is already cancelled");
    }
    booking.cancel(clock.instant());
    audit.record("BOOKING_CANCELLED", "Booking", booking.getId(), null);
  }

  @Transactional(readOnly = true)
  public List<BookingResponse> forGuest(Long guestId, AuthenticatedUser user) {
    guard.require(user, Permissions.BOOKING_READ, guestId);
    if (!guests.existsById(guestId)) {
      throw new NotFoundException("Guest", guestId);
    }
    return bookings.findByGuestIdOrderByCheckInDesc(guestId).stream()
        .map(BookingResponse::from)
        .toList();
  }

  private Booking find(Long id) {
    return bookings.findWithDetailsById(id).orElseThrow(() -> new NotFoundException("Booking", id));
  }

  private Room bookableRoom(Long roomId) {
    Room room = rooms.findById(roomId).orElseThrow(() -> new NotFoundException("Room", roomId));
    if (!room.isBookable()) {
      throw new ConflictException("Room " + room.getRoomNumber() + " is not available for booking");
    }
    return room;
  }

  /**
   * Row lock on the room, then the overlap check, then (in the caller) the write - all inside one
   * transaction. A concurrent request for the same room waits here and then sees our booking.
   */
  private void lockAndRequireFree(Room room, StayDates dates, long excludeBookingId) {
    rooms.findByIdForUpdate(room.getId());
    if (bookings.countOverlapping(room.getId(), dates.checkIn(), dates.checkOut(), excludeBookingId)
        > 0) {
      throw new ConflictException(
          "Room " + room.getRoomNumber() + " is already booked for the selected dates");
    }
  }

  private PricingQuote quote(Room room, StayDates dates, String currency) {
    return pricing.quote(
        dates, availability.occupancyPercent(dates), Set.of(room.getRoomType()), currency);
  }
}
