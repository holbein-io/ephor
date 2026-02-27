#!/bin/sh
set -e

: ${API_URL:=http://sentinel-api:8080}

echo "Configuring nginx with API_URL: ${API_URL}"

envsubst '${API_URL}' < /etc/nginx/nginx.conf.template > /tmp/nginx.conf

if ! nginx -t -c /tmp/nginx.conf; then
    echo "ERROR: nginx configuration test failed!"
    exit 1
fi

echo "nginx configuration validated successfully"

exec nginx -c /tmp/nginx.conf -g 'daemon off;'
