#!/usr/bin/env bash
# End-to-end check against the running stack (through the gateway, like the UI).
#   ./scripts/smoke-test.sh [http://localhost:8080]
set -euo pipefail
BASE=${1:-http://localhost:8080}
SID="smoke-$(date +%s)-session"
J='Content-Type: application/json'

need() { command -v "$1" >/dev/null || { echo "missing: $1"; exit 1; }; }
need curl; need jq

echo "Waiting for gateway health..."
until curl -sf "$BASE/actuator/health" >/dev/null; do sleep 2; done

login() {
  curl -sf -X POST "$BASE/api/auth/login" -H "$J" -H "X-Session-Id: $SID" \
    -d "{\"username\":\"$1\",\"password\":\"$2\"}" | jq -r .token
}
GUEST=$(login guest1 'Guest123!')
ADMIN=$(login admin 'Admin123!')
IN=$(date -u -v+20d +%F 2>/dev/null || date -u -d '+20 days' +%F)
OUT=$(date -u -v+22d +%F 2>/dev/null || date -u -d '+22 days' +%F)
H=(-H "Authorization: Bearer $GUEST" -H "X-Session-Id: $SID" -H "$J")

echo "1) availability $IN..$OUT"
AV=$(curl -sf "$BASE/api/rooms/availability?checkin=$IN&checkout=$OUT" "${H[@]}")
echo "$AV" | jq -c '{rooms: (.rooms|length), first: .rooms[0], fx}'
ROOM=$(echo "$AV" | jq -r '.rooms[0].roomId')

echo "2) create booking (room $ROOM)"
RES=$(curl -si -X POST "$BASE/api/bookings" "${H[@]}" -d "{\"roomId\":$ROOM,\"checkIn\":\"$IN\",\"checkOut\":\"$OUT\"}")
TX=$(echo "$RES" | tr -d '\r' | awk -F': ' 'tolower($1)=="x-transaction-id"{print $2; exit}')
ID=$(echo "$RES" | sed -n '/^{/,$p' | jq -r .id)
echo "   booking=$ID tx=$TX"

echo "3) double booking -> expect 409"
curl -s -o /dev/null -w "   %{http_code}\n" -X POST "$BASE/api/bookings" "${H[@]}" \
  -d "{\"roomId\":$ROOM,\"checkIn\":\"$IN\",\"checkOut\":\"$OUT\"}"

echo "4) another guest's booking list -> expect 403"
curl -s -o /dev/null -w "   %{http_code}\n" "$BASE/api/guests/2/bookings" "${H[@]}"

echo "5) update + cancel"
curl -sf -X PUT "$BASE/api/bookings/$ID" "${H[@]}" -d "{\"roomId\":$ROOM,\"checkIn\":\"$IN\",\"checkOut\":\"$OUT\"}" | jq -c '{id,status,totalAmountMinor,currency}'
curl -s -o /dev/null -w "   delete %{http_code}\n" -X DELETE "$BASE/api/bookings/$ID" "${H[@]}"

echo "6) audit trail of the create transaction (admin)"
curl -sf "$BASE/api/audit?transactionId=$TX" -H "Authorization: Bearer $ADMIN" | jq -c '.[] | {action, entityType, userId, sessionId}'
echo "7) whole session journey (admin)"
curl -sf "$BASE/api/audit?sessionId=$SID&limit=20" -H "Authorization: Bearer $ADMIN" | jq -r '.[] | "   \(.timestamp) \(.action) \(.outcome // "")"'
echo "OK"
