# 🔧 Development stage - s hot reload podporou
FROM node:20-alpine AS development
WORKDIR /app

# 🔧 FIX: Přidáme wget pro healthcheck
RUN apk add --no-cache wget

# Kopíruj package files
COPY package*.json ./

# Install dependencies
RUN npm install --legacy-peer-deps

# Kopíruj zbytek aplikace
COPY . .

# 🔥 Důležité pro hot reload v Docker
ENV CHOKIDAR_USEPOLLING=true
ENV WATCHPACK_POLLING=true
ENV FAST_REFRESH=true

# Expose port pro Vite dev server
EXPOSE 3000

# Start dev server s hot reload
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0", "--port", "3000"]

# 🏗️ Build stage pro produkci
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm install --legacy-peer-deps
COPY . .
RUN npm run build

# 🚀 Production stage
FROM nginx:alpine AS production
COPY --from=builder /app/dist /usr/share/nginx/html

# Kopíruj Nginx config pro produkci
COPY docker/frontend/nginx.conf /etc/nginx/nginx.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]