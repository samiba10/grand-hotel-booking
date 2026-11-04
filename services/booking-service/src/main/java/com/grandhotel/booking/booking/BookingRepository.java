package com.grandhotel.booking.booking;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

  /** Two stays overlap when each starts before the other ends: [a,b) ∩ [c,d) ⇔ a < d ∧ c < b. */
  @Query(
      """
      select count(b) from Booking b
      where b.room.id = :roomId
        and b.status = com.grandhotel.booking.booking.BookingStatus.CONFIRMED
        and b.checkIn < :checkOut
        and b.checkOut > :checkIn
        and b.id <> :excludeId
      """)
  long countOverlapping(
      @Param("roomId") Long roomId,
      @Param("checkIn") LocalDate checkIn,
      @Param("checkOut") LocalDate checkOut,
      @Param("excludeId") Long excludeId);

  @Query(
      """
      select distinct b.room.id from Booking b
      where b.status = com.grandhotel.booking.booking.BookingStatus.CONFIRMED
        and b.checkIn < :checkOut
        and b.checkOut > :checkIn
      """)
  Set<Long> findBookedRoomIds(
      @Param("checkIn") LocalDate checkIn, @Param("checkOut") LocalDate checkOut);

  @EntityGraph(attributePaths = {"guest", "room"})
  Optional<Booking> findWithDetailsById(Long id);

  @EntityGraph(attributePaths = {"guest", "room"})
  List<Booking> findByGuestIdOrderByCheckInDesc(Long guestId);
}
