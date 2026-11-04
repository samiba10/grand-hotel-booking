#!/usr/bin/env bash
# One-time Railway configuration of all services (safe to re-run).
# Prereq: railway login && railway link, with services fx-service, pricing-service,
# booking-service, auth-service, gateway-service, frontend and Postgres booking-db, fx-db.
set -euo pipefail

JWT_SECRET=${JWT_SECRET:-$(openssl rand -base64 48 | tr -d '\n')}
JAVA_OPTS='-Xmx256m -XX:MaxMetaspaceSize=160m -Xss512k -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError'

set_vars() {
  local svc=$1; shift
  local args=()
  for kv in "$@"; do args+=(--set "$kv"); done
  railway variables --service "$svc" --skip-deploys "${args[@]}" >/dev/null
  echo "configured $svc"
}

java=( "PORT=8080" "SERVER_PORT=8080" "JWT_SECRET=$JWT_SECRET" "JAVA_OPTS=$JAVA_OPTS" )

# ${{service.VAR}} are Railway reference variables, resolved by Railway (single quotes on purpose).
set_vars fx-service "${java[@]}" "SERVICE=fx-service" \
  'DB_URL=jdbc:postgresql://${{fx-db.PGHOST}}:${{fx-db.PGPORT}}/${{fx-db.PGDATABASE}}' \
  'DB_USERNAME=${{fx-db.PGUSER}}' 'DB_PASSWORD=${{fx-db.PGPASSWORD}}'

set_vars pricing-service "${java[@]}" "SERVICE=pricing-service" \
  'FX_URL=http://${{fx-service.RAILWAY_PRIVATE_DOMAIN}}:8080'

set_vars booking-service "${java[@]}" "SERVICE=booking-service" \
  'DB_URL=jdbc:postgresql://${{booking-db.PGHOST}}:${{booking-db.PGPORT}}/${{booking-db.PGDATABASE}}' \
  'DB_USERNAME=${{booking-db.PGUSER}}' 'DB_PASSWORD=${{booking-db.PGPASSWORD}}' \
  'PRICING_URL=http://${{pricing-service.RAILWAY_PRIVATE_DOMAIN}}:8080'

set_vars auth-service "${java[@]}" "SERVICE=auth-service"

set_vars gateway-service "${java[@]}" "SERVICE=gateway-service" \
  'AUTH_URL=http://${{auth-service.RAILWAY_PRIVATE_DOMAIN}}:8080' \
  'BOOKING_URL=http://${{booking-service.RAILWAY_PRIVATE_DOMAIN}}:8080' \
  'CORS_ALLOWED_ORIGINS=https://${{frontend.RAILWAY_PUBLIC_DOMAIN}}' \
  'RATE_LIMIT_TRUST_XFF=true'

set_vars frontend "PORT=8080" \
  'API_UPSTREAM=http://${{gateway-service.RAILWAY_PRIVATE_DOMAIN}}:8080'

echo "Done. JWT_SECRET is stored only in Railway."
