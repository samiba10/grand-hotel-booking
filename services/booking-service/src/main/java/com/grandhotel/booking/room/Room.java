package com.grandhotel.booking.room;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rooms")
public class Room {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "hotel_id", nullable = false)
  private Long hotelId;

  @Column(name = "room_number", nullable = false, length = 10)
  private String roomNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "room_type", nullable = false, length = 20)
  private RoomType roomType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RoomStatus status;

  protected Room() {}

  public Long getId() {
    return id;
  }

  public Long getHotelId() {
    return hotelId;
  }

  public String getRoomNumber() {
    return roomNumber;
  }

  public RoomType getRoomType() {
    return roomType;
  }

  public RoomStatus getStatus() {
    return status;
  }

  public boolean isBookable() {
    return status == RoomStatus.ACTIVE;
  }
}
