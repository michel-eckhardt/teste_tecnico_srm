# syntax=docker/dockerfile:1

# ---- build: type-check and bundle the SPA -------------------------------------------------------
FROM node:25-alpine AS build
WORKDIR /app

# Dependencies first (cached layer while package*.json do not change); exact versions from the lock.
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund

COPY . .
RUN npm run build

# ---- runtime: static files + reverse proxy, as a non-root user ----------------------------------
FROM nginxinc/nginx-unprivileged:1.30-alpine

# Backend reached through the same origin (/api -> API_UPSTREAM). NGINX_RESOLVER is Docker's
# embedded DNS, used to resolve the upstream at request time.
ENV API_UPSTREAM=http://backend:8080 \
    NGINX_RESOLVER=127.0.0.11

COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/healthz || exit 1
