# 📖 API Documentation - `/api/journal/scan`

## 🎯 Endpoint Overview

**`POST /api/journal/scan`** - Handwritten journal page image-ஐ upload செய்து, OCR-ஆல் text extract செய்து, tasks, events, notes, emotions-ஐ automatically detect செய்யும் endpoint.

---

## ✨ என்ன செய்கிறது? (What it does)

1. **Image Upload** - Journal page image-ஐ server-ல் store செய்கிறது
2. **OCR Processing** - Tesseract OCR-ஆல் handwritten text-ஐ extract செய்கிறது
3. **Content Extraction** - Extracted text-லிருந்து automatically:
   - **Tasks** (•, X, / symbols)
   - **Events** (O, ⦿ symbols)
   - **Notes** (free text)
   - **Emotions** (emotion keywords)
4. **Database Storage** - Image path, extracted text, மற்றும் parsed content-ஐ save செய்கிறது

---

## 📋 Request Format

### Method
```
POST
```

### URL
```
http://localhost:8080/api/journal/scan
```

### Headers
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: multipart/form-data
```

### Request Body (Form Data)
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `image` | File | ✅ Yes | Journal page image (JPG, PNG, etc.) |
| `pageNumber` | Integer | ❌ Optional | Page number (default: 1) |
| `threadId` | String | ❌ Optional | Related pages-ஐ link செய்ய (e.g., "2025-12-06") |

---

## 📤 Response Format

### Success Response (201 Created)
```json
{
  "journalPageId": 1,
  "imagePath": "uploads/1/journal-page-20251206-123456.jpg",
  "originalFilename": "my-journal-page.jpg",
  "pageNumber": 1,
  "threadId": "2025-12-06",
  "scannedAt": "2025-12-06T10:30:00",
  "extractedText": "• Buy groceries\nX Complete project\n/ Review document\nO Meeting on 12/25\nFeeling grateful today",
  "message": "Page scanned and saved successfully. OCR extracted 85 characters. Extracted: 3 tasks, 1 events, 0 notes, 1 emotions"
}
```

### Error Responses

**400 Bad Request** - Invalid file or missing image
```json
{
  "message": "Image file is required"
}
```

**401 Unauthorized** - Missing or invalid JWT token
```json
{
  "error": "Unauthorized"
}
```

**500 Internal Server Error** - File save or OCR processing failed
```json
{
  "message": "Error saving file: ..."
}
```

---

## 🔧 எப்படி Request Send பண்ணுவது? (How to Send Request)

### 1. Using cURL
```bash
curl -X POST http://localhost:8080/api/journal/scan \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "image=@/path/to/journal-page.jpg" \
  -F "pageNumber=1" \
  -F "threadId=2025-12-06"
```

### 2. Using Postman
1. Method: **POST**
2. URL: `http://localhost:8080/api/journal/scan`
3. Headers:
   - `Authorization: Bearer YOUR_JWT_TOKEN`
4. Body → form-data:
   - `image` (File) → Select your image file
   - `pageNumber` (Text) → `1` (optional)
   - `threadId` (Text) → `2025-12-06` (optional)
5. Click **Send**

### 3. Using JavaScript (Fetch API)
```javascript
const formData = new FormData();
formData.append('image', fileInput.files[0]);
formData.append('pageNumber', 1);
formData.append('threadId', '2025-12-06');

fetch('http://localhost:8080/api/journal/scan', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer YOUR_JWT_TOKEN'
  },
  body: formData
})
.then(response => response.json())
.then(data => {
  console.log('Success:', data);
  console.log('Extracted Text:', data.extractedText);
  console.log('Message:', data.message);
})
.catch(error => {
  console.error('Error:', error);
});
```

### 4. Using React/Next.js
```jsx
const handleScan = async (file) => {
  const formData = new FormData();
  formData.append('image', file);
  formData.append('pageNumber', 1);
  formData.append('threadId', new Date().toISOString().split('T')[0]);

  try {
    const response = await fetch('http://localhost:8080/api/journal/scan', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      body: formData
    });

    const data = await response.json();
    if (response.ok) {
      console.log('Scanned successfully:', data);
      // Show success message
      alert(data.message);
    } else {
      console.error('Error:', data.message);
    }
  } catch (error) {
    console.error('Network error:', error);
  }
};
```

### 5. Using Python (requests)
```python
import requests

url = "http://localhost:8080/api/journal/scan"
headers = {
    "Authorization": "Bearer YOUR_JWT_TOKEN"
}
files = {
    'image': ('journal-page.jpg', open('journal-page.jpg', 'rb'), 'image/jpeg')
}
data = {
    'pageNumber': 1,
    'threadId': '2025-12-06'
}

response = requests.post(url, headers=headers, files=files, data=data)
print(response.json())
```

---

## 💡 எதுக்கு Use பண்ணலாம்? (Use Cases)

### 1. **Daily Journal Scanning**
- Daily journal pages-ஐ scan செய்து digital-ஆக convert செய்ய
- Handwritten notes-ஐ searchable text-ஆக convert செய்ய

### 2. **Task Management**
- Bullet journal tasks (•, X, /) automatically detect ஆகும்
- Tasks-ஐ database-ல் store செய்ய, later search/export செய்யலாம்

### 3. **Event Tracking**
- Scheduled events (O) மற்றும் completed events (⦿) track செய்ய
- Calendar integration-க்கு use செய்யலாம்

### 4. **Emotion Journaling**
- Daily emotions record செய்ய
- Emotion patterns analyze செய்ய

### 5. **Note Organization**
- Random notes-ஐ organize செய்ய
- Search functionality-ஆல் later find செய்யலாம்

### 6. **Export & Backup**
- Scanned pages-ஐ TaskPaper/Markdown format-ல் export செய்ய
- Digital backup create செய்ய

---

## 📝 Example Journal Page Format

Your handwritten journal page should contain:

```
• Buy groceries
X Complete project report
/ Review document
O Meeting on 12/25/2025
⦿ Conference completed
Feeling grateful today
Random note about the day
```

**Symbols Detected:**
- `•` → Task (TODO)
- `X` → Task (COMPLETED)
- `/` → Task (IN_PROGRESS)
- `O` → Event (SCHEDULED)
- `⦿` → Event (COMPLETED)
- Text without symbols → Note or Emotion

---

## 🔍 After Scanning - What Happens Next?

1. **View Scanned Pages**
   ```
   GET /api/journal/pages
   ```

2. **Get Specific Page**
   ```
   GET /api/journal/pages/{pageId}
   ```

3. **Search Content**
   ```
   GET /api/journal/search?query=meeting
   ```

4. **Get All Tasks**
   ```
   GET /api/journal/content/tasks
   ```

5. **Export to TaskPaper**
   ```
   GET /api/journal/export/taskpaper
   ```

6. **Export to Markdown**
   ```
   GET /api/journal/export/markdown
   ```

---

## ⚠️ Important Notes

1. **Authentication Required** - JWT token must be included in headers
2. **Image Format** - Supports JPG, PNG, and other common image formats
3. **OCR Accuracy** - Depends on image quality and handwriting clarity
4. **File Size** - Large images may take longer to process
5. **Tesseract Setup** - Ensure Tesseract OCR is installed on the server

---

## 🎯 Quick Test

1. **Get JWT Token** (from login endpoint)
2. **Take a photo** of your journal page
3. **Send POST request** with image file
4. **Check response** for extracted content
5. **View extracted tasks/events** using content endpoints

---

## 📚 Related Endpoints

- `GET /api/journal/pages` - List all scanned pages
- `GET /api/journal/pages/{id}` - Get specific page
- `GET /api/journal/search` - Search content
- `GET /api/journal/content/tasks` - Get all tasks
- `GET /api/journal/export/taskpaper` - Export tasks
- `GET /api/journal/export/markdown` - Export notes & emotions

