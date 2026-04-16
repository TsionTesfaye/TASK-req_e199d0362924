# Stage 1: Build Vue bundle
FROM node:18-alpine AS builder
WORKDIR /app
COPY package.json ./
RUN npm install
COPY . .
RUN npm run build

# Stage 2: TLS-terminating reverse proxy + SPA host
FROM nginx:1.25-alpine

RUN apk add --no-cache openssl

RUN rm /etc/nginx/conf.d/default.conf

# Generate a self-signed cert at build time. In a real internal deployment
# this is replaced by org-issued certificates mounted into /etc/nginx/ssl.
RUN mkdir -p /etc/nginx/ssl && \
    openssl req -x509 -nodes -days 825 -newkey rsa:2048 \
        -keyout /etc/nginx/ssl/server.key \
        -out /etc/nginx/ssl/server.crt \
        -subj "/CN=rescuehub.local" \
        -addext "subjectAltName=DNS:rescuehub.local,DNS:localhost,IP:127.0.0.1" && \
    chmod 600 /etc/nginx/ssl/server.key

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80 443

CMD ["nginx", "-g", "daemon off;"]
