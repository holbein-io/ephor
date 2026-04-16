FROM node:22-alpine@sha256:cb15fca92530d7ac113467696cf1001208dac49c3c64355fd1348c11a88ddf8f AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:alpine@sha256:3bcf852aed06467cf075c6105892e4d5a6ebbbafa0ce22d35062db9e90ddef4c AS runtime

RUN adduser -S -u 1001 -G nginx nginxuser && \
    mkdir -p /tmp/client_body_temp /tmp/proxy_temp /tmp/fastcgi_temp \
             /tmp/uwsgi_temp /tmp/scgi_temp \
             /var/cache/nginx/client_temp /var/cache/nginx/proxy_temp \
             /var/cache/nginx/fastcgi_temp /var/cache/nginx/uwsgi_temp \
             /var/cache/nginx/scgi_temp && \
    chown -R 1001:nginx /tmp/client_body_temp /tmp/proxy_temp /tmp/fastcgi_temp \
                        /tmp/uwsgi_temp /tmp/scgi_temp \
                        /var/cache/nginx /var/log/nginx /usr/share/nginx/html && \
    chmod -R 755 /tmp/client_body_temp /tmp/proxy_temp /tmp/fastcgi_temp \
                  /tmp/uwsgi_temp /tmp/scgi_temp /var/cache/nginx

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf.template
COPY entrypoint.sh /entrypoint.sh

RUN chown -R 1001:nginx /usr/share/nginx/html && \
    rm -f /docker-entrypoint.d/10-listen-on-ipv6-by-default.sh && \
    rm -f /docker-entrypoint.d/20-envsubst-on-templates.sh && \
    chmod +x /entrypoint.sh && \
    chown 1001:nginx /etc/nginx/nginx.conf.template

USER 1001

EXPOSE 8080

ENV API_URL=http://ephor-api:8080

ENTRYPOINT ["/entrypoint.sh"]
