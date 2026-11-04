package com.grandhotel.booking.room;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, Long> {

  List<Room> findByHotelIdAndStatusOrderByRoomNumberAsc(Long hotelId, RoomStatus status);

  long countByHotelIdAndStatus(Long hotelId, RoomStatus status);

  /**
   * SELECT ... FOR UPDATE on the room while a booking is written: concurrent requests for the SAME
   * room are serialized, so two guests can never both get it. Different rooms do not block.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from Room r where r.id = :id")
  Optional<Room> findByIdForUpdate(@Param("id") Long id);
}
