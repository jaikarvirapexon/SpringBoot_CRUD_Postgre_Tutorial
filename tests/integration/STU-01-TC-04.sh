#!/bin/sh
# STU-01-TC-04: GET /api/v1/student/{studentId} with a negative or zero id returns 404, not an error
set -e
BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"

STATUS_NEG=$(curl -s -o /tmp/stu01tc04-neg -w '%{http_code}' "$BASE_URL/api/v1/student/-1")
STATUS_ZERO=$(curl -s -o /tmp/stu01tc04-zero -w '%{http_code}' "$BASE_URL/api/v1/student/0")

if [ "$STATUS_NEG" != "404" ]; then
  echo "FAIL: GET /-1 expected 404, got $STATUS_NEG. Body: $(cat /tmp/stu01tc04-neg)"
  exit 1
fi
if [ "$STATUS_ZERO" != "404" ]; then
  echo "FAIL: GET /0 expected 404, got $STATUS_ZERO. Body: $(cat /tmp/stu01tc04-zero)"
  exit 1
fi

echo "PASS: -1 -> $STATUS_NEG, 0 -> $STATUS_ZERO"
