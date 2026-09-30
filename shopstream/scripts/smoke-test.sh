#!/usr/bin/env bash
#
# End-to-end check of the running stack, through the API gateway.
# Registers a user, then places three orders that take the three saga paths:
#   1. normal order          -> CONFIRMED
#   2. $5,499 laptop         -> CANCELLED (payment declined, stock released)
#   3. 3 keyboards (2 left)  -> CANCELLED (out of stock)
#
# Usage:  ./scripts/smoke-test.sh            (stack started with docker compose up)
#         BASE_URL=http://localhost:8080 ./scripts/smoke-test.sh
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
EMAIL="smoke-$(date +%s)@example.com"
PASSWORD="password123"
FAILURES=0

pass() { printf '  PASS  %s\n' "$1"; }
fail() { printf '  FAIL  %s\n' "$1"; FAILURES=$((FAILURES + 1)); }

# Tiny JSON helpers so the script needs only bash, curl and sed.
json_string() { sed -n "s/.*\"$1\":\"\([^\"]*\)\".*/\1/p" | head -1; }
json_number() { sed -n "s/[^0-9]*\"$1\":\([0-9][0-9]*\).*/\1/p" | head -1; }

echo "Waiting for ${BASE_URL} ..."
for _ in $(seq 1 60); do
  if curl -fs "${BASE_URL}/api/products?size=1" > /dev/null 2>&1; then break; fi
  sleep 3
done

echo "1) Register ${EMAIL}"
AUTH=$(curl -fs -X POST "${BASE_URL}/api/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"${EMAIL}\",\"password\":\"${PASSWORD}\",\"fullName\":\"Smoke Test\"}") || true
TOKEN=$(printf '%s' "$AUTH" | json_string token)
if [ -n "$TOKEN" ]; then pass "registered and received a JWT"; else fail "register: ${AUTH:-no response}"; exit 1; fi

echo "2) Catalog"
PRODUCTS=$(curl -fs "${BASE_URL}/api/products?size=50")
TOTAL=$(printf '%s' "$PRODUCTS" | sed -n 's/.*"totalElements":\([0-9]*\).*/\1/p')
if [ "${TOTAL:-0}" -ge 12 ]; then pass "catalog has ${TOTAL} products"; else fail "expected at least 12 products, got '${TOTAL}'"; fi

STATUS=$(curl -s -o /dev/null -w '%{http_code}' "${BASE_URL}/api/orders")
if [ "$STATUS" = "401" ]; then pass "orders require a token (401 without one)"; else fail "expected 401 without token, got ${STATUS}"; fi

place_and_wait() { # $1 = request body, $2 = expected final status, $3 = description
  local order id status=""
  order=$(curl -fs -X POST "${BASE_URL}/api/orders" \
    -H "Authorization: Bearer ${TOKEN}" -H 'Content-Type: application/json' -d "$1") || true
  id=$(printf '%s' "$order" | json_number id)
  if [ -z "$id" ]; then fail "$3: could not place order (${order:-no response})"; return; fi
  for _ in $(seq 1 30); do
    status=$(curl -fs "${BASE_URL}/api/orders/${id}" -H "Authorization: Bearer ${TOKEN}" | json_string status)
    if [ "$status" = "CONFIRMED" ] || [ "$status" = "CANCELLED" ]; then break; fi
    sleep 1
  done
  if [ "$status" = "$2" ]; then pass "$3: order ${id} ended ${status}"; else fail "$3: order ${id} ended '${status}', expected $2"; fi
}

echo "3) Saga paths"
place_and_wait '{"items":[{"productId":1,"quantity":1},{"productId":5,"quantity":2}]}' CONFIRMED "happy path"
place_and_wait '{"items":[{"productId":3,"quantity":1}]}' CANCELLED "payment declined"
place_and_wait '{"items":[{"productId":4,"quantity":3}]}' CANCELLED "out of stock"

echo "4) Notifications"
sleep 2
COUNT=$(curl -fs "${BASE_URL}/api/notifications/unread-count" -H "Authorization: Bearer ${TOKEN}" | json_number count)
if [ "${COUNT:-0}" -ge 3 ]; then pass "${COUNT} notifications created from Kafka events"; else fail "expected notifications, got '${COUNT}'"; fi

echo
if [ "$FAILURES" -eq 0 ]; then echo "All checks passed."; else echo "${FAILURES} check(s) failed."; exit 1; fi
