#!/bin/sh
# STU-01-TC-01: GET /api/v1/student/{studentId} returns 200 with full student body when the id exists
# Runner: curl against the real running server (no Playwright/pytest/k6 stack declared in ADR-0001)
set -e
BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"

# Arrange: id=1 is guaranteed to exist because StudentConfig seeds Mariam/Alex on boot.
STUDENT_ID=1

# Act
BODY=$(curl -s -w '\n%{http_code}' "$BASE_URL/api/v1/student/$STUDENT_ID")
STATUS=$(echo "$BODY" | tail -1)
PAYLOAD=$(echo "$BODY" | sed '$d')

# Assert
if [ "$STATUS" != "200" ]; then
  echo "FAIL: expected 200, got $STATUS. Body: $PAYLOAD"
  exit 1
fi
echo "$PAYLOAD" | grep -q '"id":1' || { echo "FAIL: body missing id=1. Body: $PAYLOAD"; exit 1; }
echo "$PAYLOAD" | grep -q '"name"' || { echo "FAIL: body missing name field. Body: $PAYLOAD"; exit 1; }
echo "$PAYLOAD" | grep -q '"email"' || { echo "FAIL: body missing email field. Body: $PAYLOAD"; exit 1; }
echo "$PAYLOAD" | grep -q '"dob"' || { echo "FAIL: body missing dob field. Body: $PAYLOAD"; exit 1; }
echo "$PAYLOAD" | grep -q '"age"' || { echo "FAIL: body missing computed age field. Body: $PAYLOAD"; exit 1; }

echo "PASS: $PAYLOAD"
