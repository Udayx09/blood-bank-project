# Google Cloud Run optimized Dockerfile for WhatsApp Service
FROM node:20-slim

# Install dependencies for Puppeteer/Chromium on Debian
RUN apt-get update && apt-get install -y \
    chromium \
    fonts-liberation \
    fonts-noto-color-emoji \
    libappindicator3-1 \
    libasound2 \
    libatk-bridge2.0-0 \
    libatk1.0-0 \
    libcairo2 \
    libcups2 \
    libdbus-1-3 \
    libdrm2 \
    libgbm1 \
    libglib2.0-0 \
    libgtk-3-0 \
    libnspr4 \
    libnss3 \
    libpango-1.0-0 \
    libx11-6 \
    libx11-xcb1 \
    libxcb1 \
    libxcomposite1 \
    libxdamage1 \
    libxext6 \
    libxfixes3 \
    libxrandr2 \
    libxshmfence1 \
    xdg-utils \
    wget \
    ca-certificates \
    dumb-init \
    --no-install-recommends \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean

# Set Puppeteer environment variables for Google Cloud Run
ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true
ENV PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium
ENV CHROME_BIN=/usr/bin/chromium
ENV NODE_ENV=production

# Create non-root user for security
RUN groupadd -r pptruser && useradd -r -g pptruser -G audio,video pptruser \
    && mkdir -p /home/pptruser/Downloads \
    && mkdir -p /app \
    && chown -R pptruser:pptruser /home/pptruser \
    && chown -R pptruser:pptruser /app

WORKDIR /app

# Copy package files first for better Docker layer caching
COPY --chown=pptruser:pptruser package*.json ./

# Install dependencies
RUN npm install --omit=dev && npm cache clean --force

# Copy source code
COPY --chown=pptruser:pptruser . .

# Create directory for WhatsApp auth persistence with proper permissions
RUN mkdir -p .wwebjs_auth .wwebjs_cache \
    && chown -R pptruser:pptruser .wwebjs_auth .wwebjs_cache \
    && chmod -R 777 .wwebjs_auth .wwebjs_cache

# Switch to non-root user
USER pptruser

# Expose port (Cloud Run uses PORT env variable, defaulting to 3001)
EXPOSE 3001

# Use dumb-init to handle PID 1 properly (important for Cloud Run)
ENTRYPOINT ["dumb-init", "--"]

# Start the service
CMD ["node", "index.js"]
