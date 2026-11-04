package com.grandhotel.booking;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.grandhotel.common.exception.ServiceUnavailableException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * End to end through HTTP + security + DB. Every test uses its own date window so tests never
 * share rooms (shared in-memory DB, any execution order).
 */
class BookingApiTest extends ApiTestSupport {

  private static final long ROOM_101 = 1;
  private static final long ROOM_102 = 2;
  private static final long ROOM_106_MAINTENANCE = 6;
  private static final long ROOM_201 = 7;

  private static LocalDate day(int offset) {
    return LocalDate.now().plusDays(offset);
  }

  private static Map<String, Object> stay(long roomId, LocalDate in, LocalDate out) {
    var body = new HashMap<String, Object>();
    body.put("roomId", roomId);
    body.put("checkIn", in.toString());
    body.put("checkOut", out.toString());
    return body;
  }

  private static Map<String, Object> stayFor(long guestId, long roomId, LocalDate in, LocalDate out) {
    var body = stay(roomId, in, out);
    body.put("guestId", guestId);
    return body;
  }

  // ---- AuthN ----

  @Test
  void noTokenIs401() throws Exception {
    mvc.perform(get("/api/bookings/1")).andExpect(status().isUnauthorized());
  }

  @Test
  void tamperedTokenIs401() throws Exception {
    mvc.perform(get("/api/bookings/1").header(AUTH, guest1() + "x"))
        .andExpect(status().isUnauthorized());
  }

  // ---- Availability ----

  @Test
  void availabilityListsActiveFreeRoomsWithPrice() throws Exception {
    LocalDate in = day(300);
    mvc.perform(
            get("/api/rooms/availability")
                .param("checkin", in.toString())
                .param("checkout", in.plusDays(2).toString())
                .header(AUTH, guest1()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hotelName").value("Grand Hotel"))
        .andExpect(jsonPath("$.rooms.length()").value(11)) // 12 rooms, 1 in maintenance
        .andExpect(jsonPath("$.rooms[0].totalAmountMinor").value(2 * NIGHTLY))
        .andExpect(jsonPath("$.rooms[*].roomNumber", not(hasItem("106"))));
  }

  @Test
  void availabilityFiltersByRoomType() throws Exception {
    LocalDate in = day(301);
    mvc.perform(
            get("/api/rooms/availability")
                .param("checkin", in.toString())
                .param("checkout", in.plusDays(1).toString())
                .param("roomType", "SUITE")
                .header(AUTH, guest1()))
        .andExpect(jsonPath("$.rooms.length()").value(2));
  }

  @Test
  void bookedRoomDisappearsFromAvailability() throws Exception {
    LocalDate in = day(10);
    send(post("/api/bookings"), guest1(), stay(ROOM_101, in, in.plusDays(3)))
        .andExpect(status().isCreated());

    mvc.perform(
            get("/api/rooms/availability")
                .param("checkin", in.plusDays(1).toString())
                .param("checkout", in.plusDays(2).toString())
                .header(AUTH, guest2()))
        .andExpect(jsonPath("$.rooms[*].roomNumber", not(hasItem("101"))));
  }

  @Test
  void missingDateParamIs400WithClearMessage() throws Exception {
    mvc.perform(get("/api/rooms/availability").param("checkin", day(5).toString()).header(AUTH, guest1()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Missing parameter: checkout"));
  }

  // ---- Create ----

  @Test
  void guestBooksForThemselvesAndPriceIsLocked() throws Exception {
    LocalDate in = day(20);
    send(post("/api/bookings"), guest1(), stay(ROOM_102, in, in.plusDays(2)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.guestId").value(1))
        .andExpect(jsonPath("$.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.totalAmountMinor").value(2 * NIGHTLY))
        .andExpect(jsonPath("$.currency").value("USD"))
        .andExpect(jsonPath("$.fxRateMicros").value(FX));
  }

  @Test
  void sameRoomOverlappingDatesIs409() throws Exception {
    LocalDate in = day(30);
    send(post("/api/bookings"), guest1(), stay(ROOM_201, in, in.plusDays(3)))
        .andExpect(status().isCreated());
    send(post("/api/bookings"), guest2(), stay(ROOM_201, in.plusDays(2), in.plusDays(4)))
        .andExpect(status().isConflict());
    // back-to-back is fine: check-in on the previous check-out day
    send(post("/api/bookings"), guest2(), stay(ROOM_201, in.plusDays(3), in.plusDays(4)))
        .andExpect(status().isCreated());
  }

  @Test
  void roomInMaintenanceCannotBeBooked() throws Exception {
    send(post("/api/bookings"), guest1(), stay(ROOM_106_MAINTENANCE, day(40), day(41)))
        .andExpect(status().isConflict());
  }

  @Test
  void guestCannotBookForAnotherGuest() throws Exception {
    send(post("/api/bookings"), guest1(), stayFor(2, ROOM_101, day(50), day(51)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.transactionId", notNullValue()));
  }

  @Test
  void staffBooksForAnyGuestButMustSayWhichOne() throws Exception {
    send(post("/api/bookings"), staff(), stay(ROOM_101, day(60), day(61)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("guestId is required"));
    send(post("/api/bookings"), staff(), stayFor(2, ROOM_101, day(60), day(61)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.guestName").value("John Smith"));
  }

  @Test
  void invalidInputIs400() throws Exception {
    send(post("/api/bookings"), guest1(), stay(ROOM_101, day(70), day(70)))
        .andExpect(status().isBadRequest());
    send(post("/api/bookings"), guest1(), stay(ROOM_101, day(-1), day(2)))
        .andExpect(status().isBadRequest());
    send(post("/api/bookings"), guest1(), Map.of("checkIn", day(70).toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details.length()").value(2));
  }

  @Test
  void unknownRoomIs404() throws Exception {
    send(post("/api/bookings"), guest1(), stay(999, day(80), day(81)))
        .andExpect(status().isNotFound());
  }

  @Test
  void pricingOutageIs503NotAFakePrice() throws Exception {
    doThrow(new ServiceUnavailableException("Pricing is temporarily unavailable", null))
        .when(pricing)
        .quote(any(), anyInt(), any(), anyString());
    send(post("/api/bookings"), guest1(), stay(ROOM_101, day(90), day(91)))
        .andExpect(status().isServiceUnavailable());
  }

  // ---- Read / ownership ----

  @Test
  void bookingIsVisibleToOwnerAndStaffOnly() throws Exception {
    long id = idOf(send(post("/api/bookings"), guest1(), stay(ROOM_101, day(100), day(101))));

    mvc.perform(get("/api/bookings/{id}", id).header(AUTH, guest1())).andExpect(status().isOk());
    mvc.perform(get("/api/bookings/{id}", id).header(AUTH, guest2())).andExpect(status().isForbidden());
    mvc.perform(get("/api/bookings/{id}", id).header(AUTH, staff())).andExpect(status().isOk());
  }

  @Test
  void guestHistoryIsOwnOnly() throws Exception {
    send(post("/api/bookings"), guest2(), stay(ROOM_102, day(110), day(111)))
        .andExpect(status().isCreated());

    mvc.perform(get("/api/guests/2/bookings").header(AUTH, guest2()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].guestId", not(hasItem(1))));
    mvc.perform(get("/api/guests/2/bookings").header(AUTH, guest1())).andExpect(status().isForbidden());
    mvc.perform(get("/api/guests/2").header(AUTH, guest2()))
        .andExpect(jsonPath("$.email").value("john.smith@example.com"));
  }

  // ---- Update ----

  @Test
  void updateMovesDatesAndRePrices() throws Exception {
    long id = idOf(send(post("/api/bookings"), guest1(), stay(ROOM_101, day(120), day(121))));

    send(put("/api/bookings/{id}", id), guest1(), stay(ROOM_101, day(120), day(123)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nights").value(3))
        .andExpect(jsonPath("$.totalAmountMinor").value(3 * NIGHTLY));
  }

  @Test
  void updateIntoATakenSlotIs409() throws Exception {
    send(post("/api/bookings"), guest2(), stay(ROOM_102, day(130), day(132)))
        .andExpect(status().isCreated());
    long mine = idOf(send(post("/api/bookings"), guest1(), stay(ROOM_101, day(130), day(132))));

    send(put("/api/bookings/{id}", mine), guest1(), stay(ROOM_102, day(131), day(133)))
        .andExpect(status().isConflict());
  }

  @Test
  void guestCannotUpdateSomeoneElsesBooking() throws Exception {
    long id = idOf(send(post("/api/bookings"), guest1(), stay(ROOM_101, day(140), day(141))));
    send(put("/api/bookings/{id}", id), guest2(), stay(ROOM_101, day(140), day(142)))
        .andExpect(status().isForbidden());
  }

  // ---- Cancel (DELETE) ----

  @Test
  void deleteCancelsFreesTheRoomAndIsNotRepeatable() throws Exception {
    LocalDate in = day(150);
    long id = idOf(send(post("/api/bookings"), guest1(), stay(ROOM_101, in, in.plusDays(1))));

    mvc.perform(delete("/api/bookings/{id}", id).header(AUTH, guest2())).andExpect(status().isForbidden());
    mvc.perform(delete("/api/bookings/{id}", id).header(AUTH, guest1())).andExpect(status().isNoContent());
    mvc.perform(delete("/api/bookings/{id}", id).header(AUTH, guest1())).andExpect(status().isConflict());

    mvc.perform(get("/api/bookings/{id}", id).header(AUTH, guest1()))
        .andExpect(jsonPath("$.status").value("CANCELLED"));
    send(post("/api/bookings"), guest2(), stay(ROOM_101, in, in.plusDays(1)))
        .andExpect(status().isCreated());
  }

  // ---- Audit ----

  @Test
  void stateChangesAndRequestsAreAuditedWithTransactionAndSession() throws Exception {
    var created =
        mvc.perform(
                post("/api/bookings")
                    .header(AUTH, guest1())
                    .header("X-Transaction-Id", "BOOKING_CREATE-test-0001")
                    .header("X-Transaction-Type", "BOOKING_CREATE")
                    .header("X-Session-Id", "session-test-0001")
                    .contentType("application/json")
                    .content(json.writeValueAsString(stay(ROOM_102, day(160), day(161)))))
            .andExpect(status().isCreated())
            .andExpect(header().string("X-Transaction-Id", "BOOKING_CREATE-test-0001"))
            .andExpect(header().string("X-Session-Id", "session-test-0001"));
    long id = idOf(created);

    mvc.perform(
            get("/api/audit")
                .param("entityType", "Booking")
                .param("entityId", String.valueOf(id))
                .header(AUTH, admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].action").value("BOOKING_CREATED"))
        .andExpect(jsonPath("$[0].userId").value("guest1"))
        .andExpect(jsonPath("$[0].transactionId").value("BOOKING_CREATE-test-0001"))
        .andExpect(jsonPath("$[0].sessionId").value("session-test-0001"));

    // the request itself is audited too (business event + access record share the transaction id)
    mvc.perform(get("/api/audit").param("transactionId", "BOOKING_CREATE-test-0001").header(AUTH, admin()))
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[*].entityType", hasItem("HTTP_REQUEST")));
  }

  @Test
  void onlyAuditReadersSeeTheAuditLog() throws Exception {
    mvc.perform(get("/api/audit").header(AUTH, guest1())).andExpect(status().isForbidden());
    mvc.perform(get("/api/audit").header(AUTH, staff())).andExpect(status().isForbidden());
    mvc.perform(get("/api/audit").header(AUTH, admin())).andExpect(status().isOk());
  }
}
