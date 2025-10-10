# 🚀 PRODUKČNÍ DOCKERFILE - ČISTÝ ESBUILD
FROM node:18-alpine AS build
WORKDIR /app

# Build-time environment variables
ARG STREAMING_ENABLED=false
ARG GRAFANA_PUBLIC_URL=https://grafana.core-platform.local
ARG API_BASE_URL=/api

ENV STREAMING_ENABLED=${STREAMING_ENABLED}
ENV GRAFANA_PUBLIC_URL=${GRAFANA_PUBLIC_URL}
ENV API_BASE_URL=${API_BASE_URL}

COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ .
RUN node esbuild.mjs

FROM nginx:1.25-alpine
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK CMD curl -f http://localhost/health || exit 1