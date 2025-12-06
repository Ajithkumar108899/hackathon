# 📸 Swagger UI-ல் Image Upload - Step by Step

## 🎯 Problem
Swagger UI-ல் JSON format show ஆகிறது, ஆனால் actual-ல file upload (multipart/form-data) தேவை.

## ✅ Solution
Annotations add செய்து Swagger-க்கு file upload-ஐ properly show செய்யலாம்.

---

## 📋 Swagger UI-ல் Image Upload செய்ய - Detailed Steps

### Step 1: Swagger UI Open செய்ய
```
http://localhost:8080/swagger-ui.html
```

### Step 2: Authentication Setup

1. **Login செய்ய:**
   - `POST /api/users/auth/login` endpoint-ஐ find செய்ய
   - "Try it out" click செய்ய
   - Request body:
   ```json
   {
     "username": "your_username",
     "password": "your_password"
   }
   ```
   - "Execute" click செய்ய
   - Response-லிருந்து `token` copy செய்ய

2. **Authorize செய்ய:**
   - Swagger UI-ன் மேலே **"Authorize"** 🔒 button click
   - Value field-ல்: `Bearer YOUR_TOKEN` enter
   - "Authorize" click → "Close" click

### Step 3: Image Upload செய்ய

1. **`POST /api/journal/scan` endpoint-ஐ find செய்ய**
   - "Journal Management" section-ல் இருக்கும்

2. **"Try it out" button click செய்ய**

3. **Request Body Fill செய்ய:**

   Swagger UI-ல் இப்போது **3 separate fields** show ஆகும்:

   **a) image** (required - File)
   - Type: `file` (not string!)
   - "Choose File" button click
   - Your journal page image select (JPG, PNG, etc.)
   - File name display ஆகும்

   **b) pageNumber** (optional - Integer)
   - Type: `integer`
   - Text field-ல்: `1` enter (optional)

   **c) threadId** (optional - String)
   - Type: `string`
   - Text field-ல்: `2025-12-06` enter (optional)

4. **"Execute" button click செய்ய**

### Step 4: Response Check

**Success Response:**
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

## 🔍 Visual Guide

### Before Fix (Wrong):
```
┌─────────────────────────────────────┐
│ POST /api/journal/scan              │
│                                     │
│ Request body:                       │
│ {                                   │
│   "image": "string",    ❌ Wrong!   │
│   "pageNumber": 123,                │
│   "threadId": "string"             │
│ }                                   │
└─────────────────────────────────────┘
```

### After Fix (Correct):
```
┌─────────────────────────────────────┐
│ POST /api/journal/scan              │
│                                     │
│ Parameters:                         │
│                                     │
│ image: [Choose File] 📁 ✅          │
│   [journal-page.jpg]                │
│                                     │
│ pageNumber: [____] (optional)       │
│   1                                  │
│                                     │
│ threadId: [____] (optional)         │
│   2025-12-06                        │
│                                     │
│ [Execute]                           │
└─────────────────────────────────────┘
```

---

## ⚠️ Important Notes

1. **File Upload Field:**
   - "Choose File" button-ஐ use செய்ய
   - JSON-ல் string enter செய்யாதீர்கள்
   - Actual image file select செய்ய

2. **Content Type:**
   - Automatically `multipart/form-data` set ஆகும்
   - Manual-ஆ change செய்ய தேவை இல்லை

3. **File Formats:**
   - JPG, JPEG, PNG support ஆகும்
   - Max file size: 10MB (configured)

4. **Authentication:**
   - Every request-க்கு JWT token தேவை
   - Token expire ஆனால், login செய்து new token get செய்ய

---

## 🎯 Quick Checklist

Before testing:
- ✅ Application running
- ✅ User account created
- ✅ JWT token obtained
- ✅ Token authorized in Swagger
- ✅ Image file ready (JPG/PNG)
- ✅ File size < 10MB

---

## 💡 Tips

1. **First Time:**
   - Login → Get Token → Authorize → Then scan

2. **Image Quality:**
   - Clear, high-resolution images
   - Good lighting
   - Readable text

3. **Testing:**
   - Simple journal page-ஐ start செய்ய
   - Extracted text check செய்ய
   - Tasks/events detect ஆகிறதா verify செய்ய

4. **Multiple Pages:**
   - Same `threadId` use செய்ய
   - Different `pageNumber` for each page

---

## 🔧 Troubleshooting

### Issue: "Choose File" button show ஆகவில்லை
**Solution:** Application restart செய்ய (annotations load ஆக)

### Issue: "400 Bad Request - Image file is required"
**Solution:** File select செய்யவில்லை - "Choose File" click செய்து image select

### Issue: "401 Unauthorized"
**Solution:** Token authorize செய்யவில்லை - Authorize button click செய்து token add

### Issue: "500 Internal Server Error"
**Solution:** 
- File format check (JPG/PNG)
- File size check (< 10MB)
- Server logs check

---

## ✅ After Fix

Application restart செய்த பிறகு:
- Swagger UI-ல் file upload field properly show ஆகும்
- "Choose File" button available
- multipart/form-data automatically set ஆகும்
- Image upload successfully work ஆகும்

