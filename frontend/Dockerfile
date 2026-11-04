# syntax=docker/dockerfile:1.7
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN if [ -f package-lock.json ]; then npm ci; else npm install; fi
COPY . .
RUN npm run build

# Unprivileged nginx: non-root, port 8080
FROM nginxinc/nginx-unprivileged:stable-alpine
# Config is a template: the same image runs in compose and on Railway (API_UPSTREAM).
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
ENV API_UPSTREAM=http://gateway-service:8080 \
    NGINX_ENTRYPOINT_LOCAL_RESOLVERS=1
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/ >/dev/null || exit 1
