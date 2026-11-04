package com.grandhotel.common.authz;

/**
 * Permission vocabulary: {@code <resource>:<action>[:<scope>]}. Scope {@code own} = only resources
 * owned by the caller's guest profile, {@code any} = all resources. Code checks permissions, never
 * roles, so adding a role is a configuration change.
 */
public final class Permissions {

  public static final String ROOM_READ = "room:read";
  public static final String PRICING_QUOTE = "pricing:quote";
  public static final String AUDIT_READ = "audit:read";
  public static final String FX_READ = "fx:read";

  /** Scoped actions: combine with {@link AccessGuard#require}. */
  public static final String BOOKING_CREATE = "booking:create";

  public static final String BOOKING_READ = "booking:read";
  public static final String BOOKING_UPDATE = "booking:update";
  public static final String BOOKING_CANCEL = "booking:cancel";
  public static final String GUEST_READ = "guest:read";

  public static final String OWN = ":own";
  public static final String ANY = ":any";

  private Permissions() {}
}
