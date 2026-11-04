package com.grandhotel.booking.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.grandhotel.common.exception.BadRequestException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class StayDatesTest {

  private final Clock clock = Clock.fixed(Instant.parse("2026-06-01T08:00:00Z"), ZoneOffset.UTC);
  private final LocalDate today = LocalDate.of(2026, 6, 1);

  @Test
  void countsNightsNotDays() {
    assertThat(StayDates.of(today, today.plusDays(3), clock).nights()).isEqualTo(3);
  }

  @Test
  void checkInTodayIsAllowed() {
    assertThat(StayDates.of(today, today.plusDays(1), clock).nights()).isEqualTo(1);
  }

  @Test
  void pastCheckInIsRejected() {
    assertThatThrownBy(() -> StayDates.of(today.minusDays(1), today.plusDays(1), clock))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("past");
  }

  @Test
  void checkOutMustBeAfterCheckIn() {
    assertThatThrownBy(() -> StayDates.of(today.plusDays(2), today.plusDays(2), clock))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void maximumStayIsEnforced() {
    assertThatThrownBy(() -> StayDates.of(today, today.plusDays(31), clock))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Maximum");
  }
}
