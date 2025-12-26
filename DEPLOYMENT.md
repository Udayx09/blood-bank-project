# 🚀 Blood Bank Deployment Guide

Complete step-by-step guide to deploy the Blood Bank project.

## Architecture

```
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│  Cloudflare     │   │  GCP Cloud Run  │   │  Oracle Cloud   │
│  Pages          │──▶│  (Backend)      │──▶│  VM (WhatsApp)  │
│  (Frontend)     │   │                 │   │                 │
│  FREE           │   │  Uses Credits   │   │  FREE           │
└─────────────────┘   └────────┬────────┘   └─────────────────┘
                               │
                        ┌──────▼──────┐
                        │    Neon     │
                        │  PostgreSQL │
                        │    FREE     │
                        └─────────────┘
```

---

## Prerequisites

Install these tools:
- [Git](https://git-scm.com/downloads)
- [Google Cloud CLI](https://cloud.google.com/sdk/docs/install)
- [Node.js 20+](https://nodejs.org/) (for local testing only)

---

## Step 1: Clone Repository

```bash
git clone https://github.com/Udayx09/blood-bank-project.git
cd blood-bank-project
```

---

## Step 2: Set Up Database (Neon - FREE)

1. Go to [neon.tech](https://neon.tech)
2. Sign up with GitHub
3. Click **"New Project"**
4. Project name: `bloodbank`
5. Database name: `bloodbank`
6. Region: `Asia Pacific (Singapore)`
7. Click **Create Project**
8. **Copy the connection string** - you'll need it!

Example connection string:
```
postgresql://username:password@ep-cool-name-123.ap-southeast-1.aws.neon.tech/bloodbank?sslmode=require
```

---

## Step 3: Deploy Backend (GCP Cloud Run)

### 3.1 Login to GCP
```bash
gcloud auth login
gcloud config set project YOUR_PROJECT_ID
```

### 3.2 Enable APIs
```bash
gcloud services enable run.googleapis.com
gcloud services enable cloudbuild.googleapis.com
gcloud services enable artifactregistry.googleapis.com
```

### 3.3 Deploy Backend
```bash
cd backend

gcloud run deploy bloodbank-backend \
  --source . \
  --region asia-south1 \
  --allow-unauthenticated \
  --set-env-vars "DATABASE_URL=YOUR_NEON_CONNECTION_STRING" \
  --set-env-vars "DB_USERNAME=YOUR_NEON_USERNAME" \
  --set-env-vars "DB_PASSWORD=YOUR_NEON_PASSWORD" \
  --set-env-vars "JWT_SECRET=your-super-secret-jwt-key-make-it-long" \
  --set-env-vars "CORS_ORIGINS=https://your-frontend-url.pages.dev" \
  --set-env-vars "GEMINI_API_KEY=YOUR_GEMINI_API_KEY"
```

**Save the URL it gives you!** (e.g., `https://bloodbank-backend-xxx.run.app`)

---

## Step 4: Deploy Frontend (Cloudflare Pages - FREE)

### 4.1 Update API URL

Edit `frontend/src/app/services/api.service.ts`:
```typescript
private baseUrl = 'https://bloodbank-backend-xxx.run.app/api';
```

Also update in:
- `frontend/src/app/pages/donor-portal/donor-portal.component.ts`
- `frontend/src/app/pages/bank-portal/bank-portal.component.ts`
- `frontend/src/app/components/chat-widget/chat-widget.component.ts`

Replace all `http://localhost:8080` with your Cloud Run URL.

### 4.2 Commit Changes
```bash
git add .
git commit -m "Update API URL for production"
git push
```

### 4.3 Deploy to Cloudflare

1. Go to [dash.cloudflare.com](https://dash.cloudflare.com)
2. Sign up / Login
3. Go to **Workers & Pages** → **Create**
4. Select **Pages** → **Connect to Git**
5. Select the `blood-bank-project` repository
6. Configure build:
   - **Framework preset**: None
   - **Build command**: `cd frontend && npm install && npm run build`
   - **Build output directory**: `frontend/dist/blood-bank-frontend/browser`
7. Click **Save and Deploy**
8. **Copy your URL** (e.g., `https://blood-bank-project.pages.dev`)

### 4.4 Update Backend CORS

Go back to GCP Console → Cloud Run → bloodbank-backend → Edit:
```bash
gcloud run services update bloodbank-backend \
  --region asia-south1 \
  --set-env-vars "CORS_ORIGINS=https://blood-bank-project.pages.dev"
```

---

## Step 5: Deploy WhatsApp Service (Oracle Cloud - FREE)

### 5.1 Create Oracle Cloud Account

1. Go to [oracle.com/cloud/free](https://www.oracle.com/cloud/free/)
2. Sign up (requires credit card for verification, but won't charge)
3. Select region: **India South (Mumbai)** - `ap-mumbai-1`

### 5.2 Create VM Instance

1. **Compute** → **Instances** → **Create Instance**
2. Name: `whatsapp-service`
3. Image: **Ubuntu 22.04**
4. Shape: **Ampere A1 Flex** (Always Free)
   - 1 OCPU, 6 GB RAM
5. **Add SSH Key** (generate one or use existing)
6. Click **Create**
7. Wait for status: **RUNNING**
8. Note the **Public IP Address**

### 5.3 Configure Security

1. **Networking** → **Virtual Cloud Networks**
2. Click your VCN → **Security Lists** → Default
3. Add **Ingress Rule**:
   - Source CIDR: `0.0.0.0/0`
   - Destination Port: `3001`

### 5.4 Set Up VM

```bash
# SSH into VM
ssh -i ~/.ssh/your_key ubuntu@YOUR_PUBLIC_IP

# Update system
sudo apt update && sudo apt upgrade -y

# Install Node.js 20
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs

# Install Chromium
sudo apt install -y chromium-browser

# Install PM2
sudo npm install -g pm2

# Clone repository
git clone https://github.com/Udayx09/blood-bank-project.git
cd blood-bank-project/whatsapp-service

# Install dependencies
npm install

# Set environment variable
export PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium-browser

# Start with PM2
pm2 start index.js --name whatsapp
pm2 save
pm2 startup   # Follow the command it outputs
```

### 5.5 Scan QR Code

1. Open browser: `http://YOUR_PUBLIC_IP:3001/api/whatsapp/qr`
2. Scan QR code with WhatsApp
3. Done! WhatsApp is connected.

### 5.6 Update Backend

```bash
gcloud run services update bloodbank-backend \
  --region asia-south1 \
  --update-env-vars "WHATSAPP_SERVICE_URL=http://YOUR_ORACLE_IP:3001"
```

---

## ✅ Verification Checklist

- [ ] Frontend loads at Cloudflare URL
- [ ] Can register/login as blood bank
- [ ] Can view blood inventory
- [ ] Database has data (check Neon console)
- [ ] WhatsApp QR page accessible
- [ ] WhatsApp shows ✅ connected
- [ ] Chatbot responds

---

## 🔧 Troubleshooting

### Backend returns 500 error
- Check Cloud Run logs: `gcloud logs read --service bloodbank-backend`
- Verify DATABASE_URL is correct

### Frontend shows CORS error
- Update CORS_ORIGINS in Cloud Run to match your frontend URL exactly

### WhatsApp not connecting
- Check if Chromium is installed: `chromium-browser --version`
- Check logs: `pm2 logs whatsapp`

---

## 📞 URLs Summary

| Service | URL |
|---------|-----|
| Frontend | `https://your-app.pages.dev` |
| Backend | `https://bloodbank-backend-xxx.run.app` |
| WhatsApp | `http://oracle-ip:3001` |
| Database | Neon console |

---

**🎉 Congratulations! Your Blood Bank is now live!**
