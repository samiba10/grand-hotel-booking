package com.grandhotel.booking.booking;

import com.grandhotel.booking.booking.BookingDtos.BookingResponse;
import com.grandhotel.booking.booking.BookingDtos.CreateBookingRequest;
import com.grandhotel.booking.booking.BookingDtos.UpdateBookingRequest;
import com.grandhotel.common.security.AuthenticatedUser;
import com.grandhotel.common.security.servlet.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Coarse AuthZ here ({@code @PreAuthorize}: does the caller hold the permission in ANY scope?),
 * fine AuthZ in {@link BookingService} (own vs any, on the loaded resource).
 */
@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings")
public class BookingController {

  private final BookingService bookings;

  public BookingController(BookingService bookings) {
    this.bookings = bookings;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyAuthority('booking:create:own', 'booking:create:any')")
  @Operation(summary = "Book a room. Guests book for themselves; staff pass guestId")
  public BookingResponse create(
      @Valid @RequestBody CreateBookingRequest req, @CurrentUser AuthenticatedUser user) {
    return bookings.create(req, user);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('booking:read:own', 'booking:read:any')")
  @Operation(summary = "Get a booking (own, or any for staff)")
  public BookingResponse get(@PathVariable Long id, @CurrentUser AuthenticatedUser user) {
    return bookings.get(id, user);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('booking:update:own', 'booking:update:any')")
  @Operation(summary = "Change room and/or dates; re-checks availability and re-prices")
  public BookingResponse update(
      @PathVariable Long id,
      @Valid @RequestBody UpdateBookingRequest req,
      @CurrentUser AuthenticatedUser user) {
    return bookings.update(id, req, user);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAnyAuthority('booking:cancel:own', 'booking:cancel:any')")
  @Operation(summary = "Cancel a booking (soft delete: status CANCELLED, history kept)")
  public void cancel(@PathVariable Long id, @CurrentUser AuthenticatedUser user) {
    bookings.cancel(id, user);
  }
}
