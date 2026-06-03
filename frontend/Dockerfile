# ===== Build stage =====
FROM node:22-alpine AS builder
RUN corepack enable && corepack prepare pnpm@9.12.1 --activate
WORKDIR /app

COPY package.json pnpm-lock.yaml* ./
RUN pnpm install --frozen-lockfile

COPY . .
# Vite bakes env at build time. Default to a same-origin "/api" path that the
# runtime nginx proxies to the backend; override for split-host deploys.
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN pnpm build

# ===== Runtime stage =====
FROM nginx:alpine AS runtime
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
