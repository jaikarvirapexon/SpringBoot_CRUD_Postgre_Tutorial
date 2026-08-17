#!/bin/sh
# STU-01-TC-03: GET /api/v1/student/{studentId} with a non-numeric path segment returns 400
set -e
BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"

STATUS=$(curl -s -o /tmp/stu01tc03-body -w '%{http_code}' "$BASE_URL/api/v1/student/abc")

if [ "$STATUS" != "400" ]; then
  echo "FAIL: expected 400, got $STATUS. Body: $(cat /tmp/stu01tc03-body)"
  exit 1
fi

echo "PASS: status=$STATUS body=$(cat /tmp/stu01tc03-body)"
