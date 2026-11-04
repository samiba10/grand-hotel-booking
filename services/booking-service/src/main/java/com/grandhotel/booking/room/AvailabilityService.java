package com.grandhotel.booking.room;

import com.grandhotel.booking.BookingProperties;
import com.grandhotel.booking.booking.BookingRepository;
import com.grandhotel.booking.booking.StayDates;
import com.grandhotel.booking.pricing.PricingClient;
import com.grandhotel.booking.pricing.PricingQuote;
import com.grandhotel.common.exception.NotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** "Room available if not booked for requested dates" (assignment), priced by pricing-service. */
@Service
public class AvailabilityService {

  private final RoomRepository rooms;
  private final HotelRepository hotels;
  private final BookingRepository bookings;
  private final PricingClient pricing;
  private final BookingProperties props;

  public AvailabilityService(
      RoomRepository rooms,
      HotelRepository hotels,
      BookingRepository bookings,
      PricingClient pricing,
      BookingProperties props) {
    this.rooms = rooms;
    this.hotels = hotels;
    this.bookings = bookings;
    this.pricing = pricing;
    this.props = props;
  }

  @Transactional(readOnly = true)
  public AvailabilityResponse search(StayDates dates, RoomType typeFilter) {
    Hotel hotel =
        hotels
            .findById(props.hotelId())
            .orElseThrow(() -> new NotFoundException("Hotel", props.hotelId()));
    Set<Long> booked = bookings.findBookedRoomIds(dates.checkIn(), dates.checkOut());
    List<Room> free =
        rooms.findByHotelIdAndStatusOrderByRoomNumberAsc(hotel.getId(), RoomStatus.ACTIVE).stream()
            .filter(r -> !booked.contains(r.getId()))
            .filter(r -> typeFilter == null || r.getRoomType() == typeFilter)
            .toList();
    int occupancy = occupancyPercent(dates);

    if (free.isEmpty()) {
      return new AvailabilityResponse(
          hotel.getId(), hotel.getName(), dates.checkIn(), dates.checkOut(), dates.nights(),
          occupancy, props.currency(), null, List.of());
    }
    Set<RoomType> types =
        free.stream()
            .map(Room::getRoomType)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(RoomType.class)));
    PricingQuote quote = pricing.quote(dates, occupancy, types, props.currency());

    List<RoomOffer> offers =
        free.stream()
            .map(
                r -> {
                  long total = quote.totalFor(r.getRoomType()).amountMinor();
                  return new RoomOffer(
                      r.getId(), r.getRoomNumber(), r.getRoomType(), total,
                      total / dates.nights(), quote.currency());
                })
            .toList();
    return new AvailabilityResponse(
        hotel.getId(), hotel.getName(), dates.checkIn(), dates.checkOut(), dates.nights(),
        occupancy, quote.currency(),
        new FxInfo(quote.fxRateMicros(), quote.fxAsOf(), quote.fxStale()), offers);
  }

  /** Share of bookable rooms already taken for (part of) the stay - input to dynamic pricing. */
  @Transactional(readOnly = true)
  public int occupancyPercent(StayDates dates) {
    long total = rooms.countByHotelIdAndStatus(props.hotelId(), RoomStatus.ACTIVE);
    if (total == 0) {
      return 100;
    }
    long taken = bookings.findBookedRoomIds(dates.checkIn(), dates.checkOut()).size();
    return (int) Math.min(100, taken * 100 / total);
  }

  public record AvailabilityResponse(
      Long hotelId,
      String hotelName,
      LocalDate checkIn,
      LocalDate checkOut,
      long nights,
      int occupancyPercent,
      String currency,
      FxInfo fx,
      List<RoomOffer> rooms) {}

  public record RoomOffer(
      Long roomId,
      String roomNumber,
      RoomType roomType,
      long totalAmountMinor,
      long averageNightlyMinor,
      String currency) {}

  public record FxInfo(long rateMicros, Instant asOf, boolean stale) {}
}
