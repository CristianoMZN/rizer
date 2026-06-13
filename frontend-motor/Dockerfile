# syntax=docker/dockerfile:1

# Stage 1: Build Quasar SSR
FROM node:22-alpine AS build
RUN corepack enable && corepack prepare pnpm@latest --activate
RUN pnpm config set minimumReleaseAge 0
WORKDIR /app

COPY . .
RUN pnpm install --frozen-lockfile --ignore-scripts
RUN pnpm rebuild esbuild
RUN pnpm quasar build -m ssr

# Stage 2: Production runtime
FROM node:22-alpine
WORKDIR /app

COPY --from=build /app/dist/ssr ./
RUN npm install --omit=dev

EXPOSE 3000

CMD ["node", "index.js"]
