# 📸 Swagger UI-ல் Multiple Images Upload செய்ய

## 🎯 Problem
Swagger UI-ல் single file upload field show ஆகிறது, ஆனால் multiple images upload செய்ய வேண்டும்.

## ✅ Solution
Browser-ன் file picker-ல் **Ctrl (Windows) or Cmd (Mac)** hold செய்து multiple files select செய்யலாம்.

---

## 📋 Step-by-Step Guide

### Step 1: Swagger UI Open செய்ய
```
http://localhost:8080/swagger-ui.html
```

### Step 2: `/api/journal/scan` Endpoint Find செய்ய
- "Journal Management" section-ல் `POST /api/journal/scan` endpoint-ஐ find செய்ய
- "Try it out" button click செய்ய

### Step 3: Multiple Images Select செய்ய

**Important:** Browser-ன் file picker-ல் multiple files select செய்ய:

1. **"Choose File" button click செய்ய**
2. **File picker open ஆகும்**
3. **Multiple files select செய்ய:**
   - **Windows:** `Ctrl` key hold செய்து, multiple files click செய்ய
   - **Mac:** `Cmd` key hold செய்து, multiple files click செய்ய
4. **"Open" click செய்ய**
5. **Selected files display ஆகும்** (file names show ஆகும்)

### Step 4: Other Fields Fill செய்ய

- **pageNumber:** Starting page number (optional, defaults to 1)
  - Example: `1`
  - Multiple images upload ஆனால், auto-increment ஆகும் (1, 2, 3, ...)

- **threadId:** Thread ID for linking pages (optional)
  - Example: `2025-12-06`
  - Same threadId use செய்தால், related pages link ஆகும்

### Step 5: Execute செய்ய

- **"Execute" button click செய்ய**
- Response-ல் **List of ScanResponse** return ஆகும்
- Each image-க்கு separate response

---

## 📊 Example Response

```json
[
  {
    "journalPageId": 5,
    "imagePath": "7/abc123.jpeg",
    "originalFilename": "page1.jpeg",
    "pageNumber": 1,
    "threadId": "2025-12-06",
    "scannedAt": "2025-12-06T19:47:26.784347",
    "message": "Page scanned and saved successfully. OCR extracted 199 characters. Extracted: 2 tasks, 0 events, 19 notes, 0 emotions",
    "extractedText": "..."
  },
  {
    "journalPageId": 6,
    "imagePath": "7/def456.jpeg",
    "originalFilename": "page2.jpeg",
    "pageNumber": 2,
    "threadId": "2025-12-06",
    "scannedAt": "2025-12-06T19:47:27.123456",
    "message": "Page scanned and saved successfully. OCR extracted 150 characters. Extracted: 1 task, 0 events, 10 notes, 0 emotions",
    "extractedText": "..."
  }
]
```

---

## ⚠️ Important Notes

### 1. Multiple File Selection
- **Browser Support:** Modern browsers (Chrome, Firefox, Edge, Safari) support multiple file selection
- **Method:** Hold `Ctrl` (Windows) or `Cmd` (Mac) while clicking files
- **Visual Indicator:** Selected files-ன் names display ஆகும்

### 2. Page Numbering
- **If `pageNumber` provided:** Starting page number + index
  - Example: `pageNumber = 1`, 3 images → Pages 1, 2, 3
- **If `pageNumber` not provided:** Auto-increment from 1
  - Example: 3 images → Pages 1, 2, 3

### 3. Thread ID
- **Same `threadId`:** All images same thread-ல link ஆகும்
- **Different `threadId`:** Separate threads create ஆகும்
- **Use Case:** Daily journal pages-க்கு date use செய்யலாம்

### 4. File Limits
- **Max file size:** 10MB per file
- **Allowed formats:** JPG, JPEG, PNG
- **No limit on number of files** (but consider request size)

---

## 🔧 Troubleshooting

### Issue: Only one file select ஆகிறது
**Solution:** 
- `Ctrl` (Windows) or `Cmd` (Mac) hold செய்து multiple files click செய்ய
- File picker-ல் multiple selection enable ஆக இருக்க வேண்டும்

### Issue: Selected files show ஆகவில்லை
**Solution:**
- Browser refresh செய்ய
- File picker-ல் files properly select ஆகிறதா check செய்ய
- Browser console-ல் errors check செய்ய

### Issue: "400 Bad Request - Image file is required"
**Solution:**
- At least one file select செய்ய வேண்டும்
- File format correct ஆக இருக்க வேண்டும் (JPG, PNG)

### Issue: Some files upload ஆகவில்லை
**Solution:**
- File size check செய்ய (max 10MB)
- File format check செய்ய (JPG, PNG only)
- Network connection check செய்ய

---

## 💡 Tips

1. **Batch Upload:**
   - Multiple pages-ஐ ஒரே request-ல upload செய்யலாம்
   - Same `threadId` use செய்தால், related pages link ஆகும்

2. **Page Organization:**
   - `pageNumber` provide செய்தால், manual control
   - `pageNumber` provide செய்யாதால், auto-increment

3. **Testing:**
   - First single file upload test செய்ய
   - Then multiple files upload test செய்ய
   - Response-ல் all files process ஆகிறதா verify செய்ய

---

## 🎯 Quick Checklist

Before uploading multiple images:
- ✅ Application running
- ✅ Swagger UI accessible
- ✅ Image files ready (JPG/PNG)
- ✅ File sizes < 10MB each
- ✅ Browser supports multiple file selection
- ✅ Know how to use Ctrl/Cmd for multiple selection

---

## 📝 Alternative: cURL Command

If Swagger UI-ல் multiple files select ஆகவில்லை, cURL use செய்யலாம்:

```bash
curl -X POST "http://localhost:8080/api/journal/scan" \
  -H "accept: application/json" \
  -H "Content-Type: multipart/form-data" \
  -F "image=@page1.jpeg" \
  -F "image=@page2.jpeg" \
  -F "image=@page3.jpeg" \
  -F "pageNumber=1" \
  -F "threadId=2025-12-06"
```

**Note:** Multiple `-F "image=@file"` parameters use செய்யலாம்.

---

## ✅ Summary

1. Swagger UI-ல் `/api/journal/scan` endpoint-ஐ open செய்ய
2. "Choose File" click செய்ய
3. **Ctrl (Windows) or Cmd (Mac) hold செய்து** multiple files select செய்ய
4. Other fields fill செய்ய (optional)
5. "Execute" click செய்ய
6. Response-ல் all processed images-ன் details receive செய்ய

**Multiple files upload successfully! 🎉**

