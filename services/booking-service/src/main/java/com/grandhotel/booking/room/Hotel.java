package com.grandhotel.booking.room;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "hotels")
public class Hotel {

  @Id private Long id;

  @Column(nullable = false, length = 120)
  private String name;

  protected Hotel() {}

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }
}
