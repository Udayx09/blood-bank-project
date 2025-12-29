# WhatsApp Demo Mode - Simple Node.js container
FROM node:20-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies (no Chromium needed!)
RUN npm install --omit=dev

# Copy source code
COPY . .

# Expose port
EXPOSE 3001

# Start the service
CMD ["node", "index.js"]
