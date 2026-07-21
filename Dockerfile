# --- Build stage ---
FROM node:24-alpine AS build
WORKDIR /build

COPY package.json package-lock.json* ./
RUN npm install

COPY . .
RUN npm run build

# --- Runtime stage: static files served by nginx ---
FROM nginx:1.27-alpine AS runtime

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -qO- http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
