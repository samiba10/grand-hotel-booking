package com.grandhotel.common.security;

import java.util.Set;

/**
 * Result of AuthN: who the caller is, as proven by a valid signature. Carries NO permissions -
 * deciding what the caller may do is AuthZ's job.
 *
 * @param guestId the guest profile this user owns, or null for staff accounts
 */
public record VerifiedToken(String username, Set<Role> roles, Long guestId) {}
