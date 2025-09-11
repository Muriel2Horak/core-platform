# Vývojový stage
FROM node:20 AS dev
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .

EXPOSE 3000
CMD ["npm", "run", "dev"]

# Produkční stage
FROM node:20 AS builder
WORKDIR /app
COPY . .
RUN npm install
RUN npm run build

FROM nginx:alpine AS prod
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]