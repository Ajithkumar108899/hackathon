# 🔧 Tesseract OCR Installation Guide (Windows)

## ❌ Current Status
Tesseract OCR install ஆகவில்லை. இதை install செய்ய வேண்டும்.

---

## 📥 Installation Steps

### Option 1: Official Installer (Recommended)

1. **Download Tesseract:**
   - Go to: https://github.com/UB-Mannheim/tesseract/wiki
   - Download the latest Windows installer
   - File name: `tesseract-ocr-w64-setup-5.x.x.exe` (or similar)

2. **Install:**
   - Run the installer
   - **Important:** During installation, make sure to:
     - ✅ Check "Additional language data (download)"
     - ✅ Select "English" language
     - ✅ Note the installation path (usually `C:\Program Files\Tesseract-OCR`)

3. **Verify Installation:**
   ```powershell
   tesseract --version
   ```

4. **Check tessdata location:**
   - Usually: `C:\Program Files\Tesseract-OCR\tessdata`
   - Verify `eng.traineddata` file exists there

5. **Update application.properties:**
   ```properties
   ocr.tesseract.datapath=C:\\Program Files\\Tesseract-OCR\\tessdata
   ```

---

### Option 2: Chocolatey (If you have Chocolatey)

```powershell
choco install tesseract
```

---

### Option 3: Manual Download tessdata

If Tesseract is installed but `eng.traineddata` is missing:

1. Download from: https://github.com/tesseract-ocr/tessdata
2. Download `eng.traineddata` file
3. Copy to: `C:\Program Files\Tesseract-OCR\tessdata\`

---

## 🔍 Find Your Installation

If Tesseract is installed somewhere else:

1. **Search in File Explorer:**
   - Search for "tesseract.exe" in C:\ drive
   - Or search for "tessdata" folder

2. **Check Common Locations:**
   - `C:\Tesseract-OCR\tessdata`
   - `C:\Users\YourName\AppData\Local\Programs\Tesseract-OCR\tessdata`
   - `D:\Tesseract-OCR\tessdata` (if installed on D drive)

3. **Once found, update application.properties:**
   ```properties
   ocr.tesseract.datapath=C:\\Your\\Actual\\Path\\tessdata
   ```

---

## ⚠️ Quick Fix: Make OCR Optional

If you don't want to install Tesseract right now, the application will:
- ✅ Still save the image
- ✅ Still extract content (if you provide text manually)
- ❌ But won't do automatic OCR extraction

The scan endpoint will work, but extracted text will be empty.

---

## ✅ After Installation

1. Restart the application
2. Check logs for: `Tesseract data path verified. eng.traineddata found.`
3. Test the scan endpoint again

---

## 📝 Quick Test Command

After installation, test in PowerShell:
```powershell
tesseract --version
```

Should show: `tesseract 5.x.x` or similar version.

