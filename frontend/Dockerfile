FROM node:20-slim AS builder

WORKDIR /app

COPY package*.json ./
RUN npm config set registry https://registry.npmmirror.com \
 && npm ci --loglevel=info

COPY . .

RUN npm run build && npm prune --production

# --------- Runtime ---------
FROM node:20-alpine AS runner

WORKDIR /app

# 创建用户
RUN addgroup --system --gid 1001 nodejs && adduser --system --uid 1001 nextjs

COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static
COPY --from=builder --chown=nextjs:nodejs /app/package.json ./package.json
COPY --from=builder --chown=nextjs:nodejs /app/public ./public

USER nextjs

ENV PORT=3000
EXPOSE 3000

CMD ["node", "server.js"]
