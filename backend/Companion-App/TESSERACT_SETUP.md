# 🔧 Tesseract OCR Setup Guide

## ❌ Current Error
```
Error opening data file ./eng.traineddata
Please make sure the TESSDATA_PREFIX environment variable is set to your "tessdata" directory.
Failed loading language 'eng'
Tesseract couldn't load any languages!
```

## ✅ Solution

### Step 1: Install Tesseract OCR

#### Windows:
1. Download from: https://github.com/UB-Mannheim/tesseract/wiki
2. Install to default location: `C:\Program Files\Tesseract-OCR`
3. During installation, make sure to install language data (English)

#### Linux (Ubuntu/Debian):
```bash
sudo apt-get update
sudo apt-get install tesseract-ocr
sudo apt-get install libtesseract-dev
```

#### Mac:
```bash
brew install tesseract
```

### Step 2: Find Tesseract Data Path

#### Windows:
```
C:\Program Files\Tesseract-OCR\tessdata
```

#### Linux:
```bash
# Check where tessdata is located
dpkg -L tesseract-ocr | grep tessdata
# Usually: /usr/share/tesseract-ocr/5/tessdata
# Or: /usr/share/tesseract-ocr/4.00/tessdata
```

#### Mac:
```bash
# Check location
brew info tesseract
# Usually: /usr/local/share/tessdata
# Or: /opt/homebrew/share/tessdata (Apple Silicon)
```

### Step 3: Configure in application.properties

Open `application.properties` and set the path:

#### Windows:
```properties
ocr.tesseract.datapath=C:\\Program Files\\Tesseract-OCR\\tessdata
```

#### Linux:
```properties
ocr.tesseract.datapath=/usr/share/tesseract-ocr/5/tessdata
```

#### Mac:
```properties
ocr.tesseract.datapath=/usr/local/share/tessdata
```

### Step 4: Verify Installation

Check if `eng.traineddata` file exists in the tessdata directory:
- Windows: `C:\Program Files\Tesseract-OCR\tessdata\eng.traineddata`
- Linux: `/usr/share/tesseract-ocr/5/tessdata/eng.traineddata`
- Mac: `/usr/local/share/tessdata/eng.traineddata`

### Step 5: Restart Application

After setting the path, restart the Spring Boot application.

---

## 🔍 Alternative: Auto-Detection

If you leave `ocr.tesseract.datapath=` empty in `application.properties`, the application will try to auto-detect common paths:

**Windows:**
- `C:\Program Files\Tesseract-OCR\tessdata`
- `C:\Tesseract-OCR\tessdata`

**Linux:**
- `/usr/share/tesseract-ocr/5/tessdata`
- `/usr/share/tesseract-ocr/4.00/tessdata`
- `/usr/local/share/tessdata`

**Mac:**
- `/usr/local/share/tessdata`
- `/opt/homebrew/share/tessdata`

---

## 🧪 Test Tesseract Installation

### Command Line Test:

#### Windows:
```cmd
"C:\Program Files\Tesseract-OCR\tesseract.exe" --version
```

#### Linux/Mac:
```bash
tesseract --version
```

### Test OCR:
```bash
tesseract image.jpg output.txt -l eng
```

---

## ⚠️ Troubleshooting

### Issue: "Failed loading language 'eng'"
**Solution:**
- Check if `eng.traineddata` exists in tessdata directory
- Verify the path in `application.properties`
- Make sure path uses forward slashes (/) or escaped backslashes (\\)

### Issue: "Invalid memory access"
**Solution:**
- Usually caused by missing tessdata path
- Set correct path in `application.properties`
- Restart application

### Issue: Path not found
**Solution:**
1. Find exact tessdata location:
   - Windows: Check `C:\Program Files\Tesseract-OCR\tessdata`
   - Linux: `dpkg -L tesseract-ocr | grep tessdata`
   - Mac: `brew info tesseract`
2. Update `application.properties` with exact path
3. Restart application

---

## 📝 Quick Fix for Current Error

1. **Find your Tesseract installation:**
   - Windows: Usually `C:\Program Files\Tesseract-OCR\tessdata`
   - Check if this folder exists

2. **Update application.properties:**
   ```properties
   ocr.tesseract.datapath=C:\\Program Files\\Tesseract-OCR\\tessdata
   ```
   (Use double backslashes `\\` for Windows paths)

3. **Restart application**

4. **Test again with curl:**
   ```bash
   curl -X 'POST' \
     'http://localhost:8080/api/journal/scan' \
     -H 'Authorization: Bearer YOUR_TOKEN' \
     -F 'image=@your-image.jpg' \
     -F 'pageNumber=1'
   ```

---

## ✅ After Setup

Once configured correctly, you should see in logs:
```
Tesseract data path set to: C:\Program Files\Tesseract-OCR\tessdata
Tesseract language set to: eng
Starting OCR extraction for file: ...
OCR extraction completed. Extracted X characters
```

Then the image upload should work successfully! 🎉

