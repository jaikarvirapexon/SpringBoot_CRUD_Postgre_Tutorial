#!/bin/sh
# STU-01-TC-05: 404 response body for a missing student contains no stack trace or internal exception identifier
set -e
BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
STUDENT_ID=999

BODY=$(curl -s "$BASE_URL/api/v1/student/$STUDENT_ID")

# Forbidden substrings: stack trace markers, exception class names, internal identifiers.
for token in "Exception" "\.java:" "at com.example" "trace" "Caused by"; do
  case "$BODY" in
    *"$token"*)
      echo "FAIL: forbidden token '$token' found in body: $BODY"
      exit 1
      ;;
  esac
done

echo "PASS: body contains no stack trace / exception identifiers. Body: '$BODY'"
