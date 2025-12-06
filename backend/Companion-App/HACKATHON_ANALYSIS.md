# Bullet Journal Companion App - Hackathon Analysis & Implementation Plan

## 📋 Current State Analysis

### ✅ What You Already Have:
1. **Authentication & Authorization**
   - JWT-based authentication
   - User registration/login
   - Role-based access control
   - Session management

2. **Infrastructure**
   - Spring Boot 3.4.12
   - MySQL database configured
   - File upload support (multipart enabled, 10MB max)
   - Swagger/OpenAPI documentation
   - Spring AI dependency (Stability AI - currently disabled)

3. **Existing Features**
   - User management (CRUD operations)
   - JWT token generation/refresh
   - Password encryption
   - Rate limiting support

### ❌ What Needs to Be Built:

## 🎯 Core Requirements Breakdown

### 1. **Image Scanning & Upload** 📸
**Status:** Partially Ready (file upload configured)
**Needs:**
- Image upload endpoint (`/api/journal/scan`)
- Support for: JPG, PNG, PDF
- Image storage (local filesystem or cloud)
- Image preprocessing (rotation, enhancement)

### 2. **OCR & Content Extraction** 🔍
**Status:** Not Started
**Needs:**
- OCR library integration (choose one):
  - **Tesseract OCR** (free, open-source)
  - **Google Cloud Vision API** (better accuracy, paid)
  - **Spring AI Vision** (if available)
- Extract text from handwritten pages
- Symbol detection:
  - `•` (dot) → Task
  - `X` → Completed task
  - `/` → In-progress task
  - `O` → Scheduled event
  - `⦿` (filled O) → Completed event

### 3. **Data Models** 📊
**Status:** Not Started
**Needs to Create:**

```java
// JournalPage.java - Store scanned page info
- id, userId, imagePath, scannedAt, pageNumber

// Task.java - Tasks extracted from pages
- id, userId, journalPageId, content, status (TODO/IN_PROGRESS/COMPLETED)
- symbol, createdAt, updatedAt

// Event.java - Scheduled events
- id, userId, journalPageId, content, eventDate
- status (SCHEDULED/COMPLETED), symbol

// Note.java - Free text notes
- id, userId, journalPageId, content, createdAt

// Emotion.java - Emotion entries
- id, userId, journalPageId, content, emotionType, createdAt
```

### 4. **Export Functionality** 📤
**Status:** Not Started
**Needs:**

#### a. TaskPaper Format Export
```
Project Name
  - Task 1
  - Task 2 @done
  - Task 3 @inprogress
```

#### b. Markdown Export
- Notes as `.md` files
- Emotions as `.md` files
- Organized by date/user

#### c. Task Management System Integration
- **Google Tasks API** (OAuth2 required)
- **Microsoft To Do API** (OAuth2 required)
- **Notion API** (API key required)

### 5. **Search & Query** 🔎
**Status:** Not Started
**Needs:**
- Search endpoint: `/api/journal/search?q=keyword`
- Search across:
  - Tasks
  - Notes
  - Emotions
  - Events
- Filter by:
  - Date range
  - Status
  - Type (task/note/emotion/event)

### 6. **Update Mechanism** 🔄
**Status:** Not Started
**Needs:**
- Duplicate detection algorithm:
  - Compare content similarity
  - Match by position on page
  - Use fuzzy matching
- Update logic:
  - If task exists → update status
  - If note exists → update content
  - If new → create entry

### 7. **Optional: Threading Support** 🧵
**Status:** Not Started
**Needs:**
- Detect continuation markers
- Link related entries across pages
- Thread ID tracking

---

## 🛠️ Implementation Plan

### Phase 1: Foundation (MVP Core)
1. **Create Data Models**
   - JournalPage, Task, Event, Note, Emotion entities
   - Repositories for each
   - Relationships (User → JournalPage → Tasks/Notes/etc.)

2. **Image Upload Endpoint**
   - POST `/api/journal/upload`
   - Save image to filesystem
   - Store metadata in database

3. **Basic OCR Integration**
   - Add Tesseract OCR dependency
   - Extract text from images
   - Store raw extracted text

### Phase 2: Content Extraction
4. **Symbol Detection & Parsing**
   - Regex patterns for symbols
   - Parse extracted text
   - Categorize: Task/Event/Note/Emotion

5. **Data Storage**
   - Save extracted items to database
   - Link to JournalPage

### Phase 3: Export & Integration
6. **TaskPaper Export**
   - Generate TaskPaper format
   - Download as `.taskpaper` file

7. **Markdown Export**
   - Generate markdown files
   - Organize by date

8. **Task Management Integration** (Choose 1-2)
   - Google Tasks API integration
   - OR Microsoft To Do
   - OR Notion API

### Phase 4: Search & Update
9. **Search Functionality**
   - Full-text search
   - Filtering options

10. **Update Mechanism**
    - Duplicate detection
    - Update existing entries
    - Version tracking

### Phase 5: Optional Features
11. **Threading Support**
    - Continuation detection
    - Thread linking

---

## 📦 Required Dependencies

### Add to `pom.xml`:

```xml
<!-- OCR - Tesseract -->
<dependency>
    <groupId>net.sourceforge.tess4j</groupId>
    <artifactId>tess4j</artifactId>
    <version>5.8.0</version>
</dependency>

<!-- Image Processing -->
<dependency>
    <groupId>org.imgscalr</groupId>
    <artifactId>imgscalr-lib</artifactId>
    <version>4.2</version>
</dependency>

<!-- Google Tasks API (if using) -->
<dependency>
    <groupId>com.google.api-client</groupId>
    <artifactId>google-api-client</artifactId>
    <version>2.2.0</version>
</dependency>
<dependency>
    <groupId>com.google.apis</groupId>
    <artifactId>google-api-services-tasks</artifactId>
    <version>v1-rev20210709-1.32.1</version>
</dependency>

<!-- String Similarity (for duplicate detection) -->
<dependency>
    <groupId>info.debatty</groupId>
    <artifactId>java-string-similarity</artifactId>
    <version>2.0.0</version>
</dependency>
```

---

## 🗂️ Recommended Project Structure

```
src/main/java/com/bulletjournal/Companion/App/
├── controller/
│   ├── JournalController.java          # Image upload, search
│   └── ExportController.java          # Export endpoints
├── service/
│   ├── ImageProcessingService.java     # OCR, image preprocessing
│   ├── ContentExtractionService.java   # Parse symbols, categorize
│   ├── ExportService.java              # TaskPaper, Markdown export
│   ├── TaskManagementService.java      # Google Tasks/Notion integration
│   └── DuplicateDetectionService.java  # Find and update duplicates
├── model/
│   ├── JournalPage.java
│   ├── Task.java
│   ├── Event.java
│   ├── Note.java
│   └── Emotion.java
├── repository/
│   ├── JournalPageRepository.java
│   ├── TaskRepository.java
│   ├── EventRepository.java
│   ├── NoteRepository.java
│   └── EmotionRepository.java
└── dto/
    ├── ScanRequest.java
    ├── ScanResponse.java
    ├── ExportRequest.java
    └── SearchRequest.java
```

---

## 🎯 MVP Priority Features

### Must Have (Core):
1. ✅ Image upload
2. ✅ Basic OCR (Tesseract)
3. ✅ Symbol detection (•, X, /, O)
4. ✅ Store tasks/notes/events
5. ✅ TaskPaper export
6. ✅ Markdown export
7. ✅ Basic search

### Should Have:
8. ✅ Duplicate detection & update
9. ✅ One task management integration (Google Tasks)

### Nice to Have:
10. ✅ Threading support
11. ✅ Multiple task management integrations
12. ✅ Advanced search filters

---

## 🚀 Quick Start Implementation Order

1. **Create Models** (30 min)
2. **Image Upload Endpoint** (1 hour)
3. **Tesseract OCR Integration** (2 hours)
4. **Symbol Parser** (2 hours)
5. **TaskPaper Export** (1 hour)
6. **Markdown Export** (1 hour)
7. **Search Endpoint** (1 hour)
8. **Duplicate Detection** (2 hours)

**Total MVP Time:** ~10-12 hours

---

## 💡 Technology Recommendations

### OCR Options:
1. **Tesseract OCR** ⭐ Recommended for MVP
   - Free, open-source
   - Good for printed text
   - Moderate accuracy for handwriting
   - Easy to integrate

2. **Google Cloud Vision API**
   - Better accuracy
   - Handles handwriting well
   - Requires API key & billing
   - More complex setup

3. **Spring AI Vision** (if available)
   - Native Spring integration
   - May require API keys

### Task Management APIs:
1. **Google Tasks** - Good OAuth2 docs
2. **Microsoft To Do** - Microsoft Graph API
3. **Notion API** - Simple API key auth

---

## 📝 Next Steps

1. Review this analysis
2. Decide on OCR solution (Tesseract recommended for MVP)
3. Create data models
4. Start with image upload endpoint
5. Integrate OCR
6. Build symbol parser
7. Implement export features
8. Add search
9. Implement duplicate detection

---

## 🎨 API Endpoints to Create

```
POST   /api/journal/scan              - Upload & scan image
GET    /api/journal/pages             - List all scanned pages
GET    /api/journal/pages/{id}        - Get page details
GET    /api/journal/search            - Search entries
GET    /api/journal/tasks             - Get all tasks
GET    /api/journal/notes             - Get all notes
GET    /api/journal/events            - Get all events
POST   /api/journal/export/taskpaper  - Export as TaskPaper
POST   /api/journal/export/markdown   - Export as Markdown
POST   /api/journal/export/google-tasks - Export to Google Tasks
```

---

Good luck with your hackathon! 🚀

