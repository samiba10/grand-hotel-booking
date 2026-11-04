package com.grandhotel.booking.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.grandhotel.booking.ApiTestSupport;
import com.grandhotel.booking.booking.BookingDtos.CreateBookingRequest;
import com.grandhotel.common.authz.PermissionResolver;
import com.grandhotel.common.security.Role;
import com.grandhotel.common.security.VerifiedToken;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** The race the pessimistic lock exists for: N parallel requests, ONE room, ONE winner. */
class ConcurrentBookingTest extends ApiTestSupport {

  @Autowired BookingService bookings;
  @Autowired PermissionResolver permissions;

  @Test
  void parallelRequestsForTheSameRoomProduceExactlyOneBooking() throws Exception {
    var staff = permissions.toUser(new VerifiedToken("staff", Set.of(Role.STAFF), null));
    LocalDate in = LocalDate.now().plusDays(250);
    var request = new CreateBookingRequest(11L, in, in.plusDays(2), 1L); // room 301

    int threads = 8;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Boolean>> results = new ArrayList<>();
    for (int i = 0; i < threads; i++) {
      results.add(
          pool.submit(
              () -> {
                start.await();
                try {
                  bookings.create(request, staff);
                  return true;
                } catch (RuntimeException e) {
                  return false; // 409 conflict (or lock timeout) - either way, not booked
                }
              }));
    }
    start.countDown();
    int successes = 0;
    for (Future<Boolean> r : results) {
      if (r.get()) {
        successes++;
      }
    }
    pool.shutdown();

    assertThat(successes).isEqualTo(1);
  }
}
