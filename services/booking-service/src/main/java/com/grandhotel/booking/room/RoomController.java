package com.grandhotel.booking.room;

import com.grandhotel.booking.booking.StayDates;
import com.grandhotel.booking.room.AvailabilityService.AvailabilityResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
@Tag(name = "Rooms")
public class RoomController {

  private final AvailabilityService availability;
  private final Clock clock;

  public RoomController(AvailabilityService availability, Clock clock) {
    this.availability = availability;
    this.clock = clock;
  }

  @GetMapping("/availability")
  @PreAuthorize("hasAuthority('room:read')")
  @Operation(summary = "Free rooms for the stay, priced in the requested (or default) currency")
  public AvailabilityResponse availability(
      @RequestParam("checkin") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
      @RequestParam("checkout") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
      @RequestParam(required = false) RoomType roomType,
      @RequestParam(required = false) String currency) {
    return availability.search(StayDates.of(checkIn, checkOut, clock), roomType, currency);
  }
}
