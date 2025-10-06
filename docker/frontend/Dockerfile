# 🚀 PRODUKČNÍ DOCKERFILE - ČISTÝ ESBUILD
FROM node:18-alpine AS build
WORKDIR /app
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ .
RUN node esbuild.mjs

FROM nginx:1.25-alpine
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK CMD curl -f http://localhost/health || exit 1