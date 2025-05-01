#!/bin/sh

# Check if BACKEND_URL is set
if [ -z "${BACKEND_URL}" ]; then
  echo "Error: BACKEND_URL environment variable is not set." >&2
  exit 1
fi

# Replace the placeholder in app.js with the actual BACKEND_URL
# Use a temporary file to avoid issues with in-place editing
sed "s|__BACKEND_URL__|${BACKEND_URL}|g" /usr/share/nginx/html/app.js > /usr/share/nginx/html/app.js.tmp && \
mv /usr/share/nginx/html/app.js.tmp /usr/share/nginx/html/app.js

# Start nginx in the foreground
nginx -g 'daemon off;'