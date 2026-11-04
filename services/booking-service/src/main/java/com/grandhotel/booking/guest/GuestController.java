package com.grandhotel.booking.guest;

import com.grandhotel.booking.booking.BookingDtos.BookingResponse;
import com.grandhotel.booking.booking.BookingService;
import com.grandhotel.common.authz.AccessGuard;
import com.grandhotel.common.authz.Permissions;
import com.grandhotel.common.exception.NotFoundException;
import com.grandhotel.common.security.AuthenticatedUser;
import com.grandhotel.common.security.servlet.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guests")
@Tag(name = "Guests")
public class GuestController {

  private final GuestRepository guests;
  private final BookingService bookings;
  private final AccessGuard guard;

  public GuestController(GuestRepository guests, BookingService bookings, AccessGuard guard) {
    this.guests = guests;
    this.bookings = bookings;
    this.guard = guard;
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('guest:read:own', 'guest:read:any')")
  @Transactional(readOnly = true)
  @Operation(summary = "Guest profile (own, or any for staff)")
  public GuestResponse get(@PathVariable Long id, @CurrentUser AuthenticatedUser user) {
    guard.require(user, Permissions.GUEST_READ, id);
    return guests.findById(id).map(GuestResponse::from).orElseThrow(() -> new NotFoundException("Guest", id));
  }

  @GetMapping("/{id}/bookings")
  @PreAuthorize("hasAnyAuthority('booking:read:own', 'booking:read:any')")
  @Operation(summary = "Booking history of a guest (own, or any for staff)")
  public List<BookingResponse> bookings(@PathVariable Long id, @CurrentUser AuthenticatedUser user) {
    return bookings.forGuest(id, user);
  }

  public record GuestResponse(Long id, String firstName, String lastName, String email, String phone) {

    static GuestResponse from(Guest g) {
      return new GuestResponse(g.getId(), g.getFirstName(), g.getLastName(), g.getEmail(), g.getPhone());
    }
  }
}
