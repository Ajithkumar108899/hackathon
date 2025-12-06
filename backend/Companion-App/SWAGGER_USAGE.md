# 📘 Swagger UI-ல் Image Upload எப்படி செய்வது?

## 🎯 Step-by-Step Guide

### 1️⃣ Swagger UI Open செய்ய
```
http://localhost:8080/swagger-ui.html
அல்லது
http://localhost:8080/swagger-ui/index.html
```

### 2️⃣ Authentication Setup (முதலில்)

#### Step 1: Login Endpoint-ஐ Use செய்ய
- Swagger UI-ல் `POST /api/users/auth/login` endpoint-ஐ find செய்ய
- "Try it out" click செய்ய
- Request body-ல் username, password enter செய்ய:
```json
{
  "username": "your_username",
  "password": "your_password"
}
```
- "Execute" click செய்ய
- Response-லிருந்து `token` copy செய்ய

#### Step 2: Authorize Button Click செய்ய
- Swagger UI-ன் மேலே **"Authorize"** 🔒 button-ஐ click செய்ய
- "Bearer Authentication" section-ல்:
  - Value field-ல்: `Bearer YOUR_TOKEN` enter செய்ய
  - Example: `Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`
- "Authorize" click செய்ய
- "Close" click செய்ய

### 3️⃣ Image Upload செய்ய

#### Step 1: Scan Endpoint Find செய்ய
- Swagger UI-ல் `POST /api/journal/scan` endpoint-ஐ find செய்ய
- "Journal Management" section-ல் இருக்கும்

#### Step 2: "Try it out" Click செய்ய
- Endpoint-ன் வலது பக்கம் "Try it out" button-ஐ click செய்ய

#### Step 3: Request Body Fill செய்ய

**Parameters section-ல்:**

1. **image** (required) - File upload
   - "Choose File" button-ஐ click செய்ய
   - Your journal page image-ஐ select செய்ய (JPG, PNG, etc.)
   - File path display ஆகும்

2. **pageNumber** (optional)
   - Text field-ல் page number enter செய்ய
   - Example: `1`
   - Leave empty for default (1)

3. **threadId** (optional)
   - Text field-ல் thread ID enter செய்ய
   - Example: `2025-12-06`
   - Related pages-ஐ link செய்ய

#### Step 4: Execute Click செய்ய
- "Execute" button-ஐ click செய்ய
- Response-ல் result கிடைக்கும்

### 4️⃣ Response Check செய்ய

**Success Response Example:**
```json
{
  "journalPageId": 1,
  "imagePath": "uploads/1/journal-page-20251206-123456.jpg",
  "originalFilename": "my-journal.jpg",
  "pageNumber": 1,
  "threadId": "2025-12-06",
  "scannedAt": "2025-12-06T10:30:00",
  "extractedText": "• Buy groceries\nX Complete project\n...",
  "message": "Page scanned and saved successfully. OCR extracted 85 characters. Extracted: 3 tasks, 1 events, 0 notes, 1 emotions"
}
```

---

## 📸 Visual Guide

### Swagger UI Layout:
```
┌─────────────────────────────────────────┐
│  Swagger UI - Bullet Journal API       │
│  [Authorize 🔒]                         │
├─────────────────────────────────────────┤
│  Journal Management                     │
│  ┌───────────────────────────────────┐ │
│  │ POST /api/journal/scan              │ │
│  │ Scan journal page                  │ │
│  │ [Try it out]                       │ │
│  │                                     │ │
│  │ Parameters:                         │ │
│  │ image: [Choose File] 📁             │ │
│  │ pageNumber: [____]                  │ │
│  │ threadId: [____]                   │ │
│  │                                     │ │
│  │ [Execute]                           │ │
│  └───────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

---

## ⚠️ Common Issues & Solutions

### Issue 1: "401 Unauthorized"
**Solution:**
- Authorize button-ஐ click செய்து token add செய்ய
- Token format: `Bearer YOUR_TOKEN` (Bearer space-ஐ include செய்ய)

### Issue 2: "400 Bad Request - Image file is required"
**Solution:**
- File select செய்யவில்லை
- "Choose File" button-ஐ click செய்து image select செய்ய

### Issue 3: "500 Internal Server Error"
**Solution:**
- Image format check செய்ய (JPG, PNG support ஆகும்)
- File size check செய்ய (too large ஆனால் problem ஆகலாம்)
- Server logs check செய்ய

### Issue 4: Token Expired
**Solution:**
- Login endpoint-ஐ use செய்து new token get செய்ய
- Authorize section-ல் new token add செய்ய

---

## 🎯 Quick Checklist

Before testing, ensure:
- ✅ Application running (port 8080)
- ✅ MySQL database connected
- ✅ User account created
- ✅ JWT token obtained
- ✅ Token authorized in Swagger
- ✅ Image file ready (JPG/PNG)

---

## 💡 Tips

1. **First Time Setup:**
   - Login → Get Token → Authorize → Then test scan

2. **Image Quality:**
   - Clear, high-resolution images give better OCR results
   - Good lighting in the photo
   - Text should be readable

3. **Testing:**
   - Start with a simple journal page
   - Check extracted text in response
   - Verify tasks/events are detected correctly

4. **Multiple Pages:**
   - Use same `threadId` for related pages
   - Different `pageNumber` for each page

---

## 🔗 Related Endpoints to Test

After scanning, you can test:

1. **GET /api/journal/pages** - View all scanned pages
2. **GET /api/journal/pages/{id}** - Get specific page
3. **GET /api/journal/content/tasks** - View extracted tasks
4. **GET /api/journal/search?query=keyword** - Search content
5. **GET /api/journal/export/taskpaper** - Export tasks

All these endpoints-ஐ Swagger UI-ல் test செய்யலாம்!

