# WhatsApp Microservice

Standalone Node.js service for WhatsApp notifications.
Called by the Spring Boot backend.

## Setup

```bash
cd whatsapp-service
npm install
```

## Run

```bash
npm start
# or with nodemon for development
npm run dev
```

## Endpoints

- `GET /api/whatsapp/status` - Connection status
- `GET /api/whatsapp/qr` - Get QR code for pairing
- `POST /api/whatsapp/send-confirmation` - Send reservation confirmation
- `POST /api/whatsapp/send-status-update` - Send status update
- `POST /api/whatsapp/send` - Send custom message

## Port

Runs on port 3001 by default. Change with `PORT` environment variable.
