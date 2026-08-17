#!/bin/sh
# STU-01-TC-02: GET /api/v1/student/{studentId} returns 404 (not 500) when the id does not exist
set -e
BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
STUDENT_ID=999

STATUS=$(curl -s -o /tmp/stu01tc02-body -w '%{http_code}' "$BASE_URL/api/v1/student/$STUDENT_ID")

if [ "$STATUS" != "404" ]; then
  echo "FAIL: expected 404, got $STATUS. Body: $(cat /tmp/stu01tc02-body)"
  exit 1
fi

echo "PASS: status=$STATUS body=$(cat /tmp/stu01tc02-body)"
