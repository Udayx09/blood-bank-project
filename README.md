# 🩸 Blood Bank Management System

A full-stack blood bank management application with WhatsApp notifications and AI-powered chatbot.

## 🚀 Tech Stack

| Component | Technology |
|-----------|------------|
| Frontend | Angular 18 |
| Backend | Spring Boot 3.2, Java 17 |
| Database | PostgreSQL |
| WhatsApp | Node.js + whatsapp-web.js |
| AI Chat | Google Gemini API |

## 📋 Features

- **Blood Bank Portal**: Manage inventory, donors, and reservations
- **Donor Portal**: Donor registration, profile management, donation history
- **Real-time Notifications**: WhatsApp alerts for reservations and updates
- **AI Chatbot**: Gemini-powered assistant for user queries
- **Admin Dashboard**: System-wide analytics and management

## 🛠️ Setup

### Prerequisites
- Java 17+
- Node.js 18+
- PostgreSQL
- Angular CLI

### Backend
```bash
cd backend
# Copy and configure environment variables
cp .env.example .env
# Edit .env with your values

# Run
./mvnw spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm start
# Opens at http://localhost:4200
```

### WhatsApp Service
```bash
cd whatsapp-service
npm install
npm start
# Scan QR code at http://localhost:3001/api/whatsapp/qr
```

## 🔐 Environment Variables

See `backend/.env.example` for required configuration:
- `DATABASE_URL` - PostgreSQL connection
- `JWT_SECRET` - Authentication secret
- `GEMINI_API_KEY` - Google Gemini API
- `WHATSAPP_SERVICE_URL` - WhatsApp microservice URL

## 📄 License

MIT License
