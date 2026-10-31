# นำ Type Strike ขึ้น GitHub

ไฟล์ในเครื่องพร้อมสำหรับสร้าง Repository แล้ว แต่ยังไม่ได้เชื่อมบัญชี สร้าง Repository หรือ Push ขึ้น GitHub

## วิธีที่ทำได้โดยไม่ใช้คำสั่ง Git

1. เข้าสู่ระบบ GitHub ด้วยบัญชีของตนเอง
2. สร้าง Repository ชื่อ `type-strike` เลือก Public หรือ Private ตามข้อกำหนดอาจารย์ หากเป็น Private ต้องให้ผู้ตรวจมีสิทธิ์เข้าถึง
3. อัปโหลด `src`, `resources`, `tests`, `docs`, `.vscode`, `.gitignore`, `README.md`, `build.ps1`, `run.ps1`, `test.ps1`, `Build.cmd` และ `Play.cmd`
4. ไม่อัปโหลด `build` และ `tmp` ส่วน `dist` ไม่เก็บใน Source Repository ตาม .gitignore
5. หากต้องการแจก JAR ให้สร้าง GitHub Release แล้วแนบ `dist/TypeStrike.jar` และ Source ZIP ในโฟลเดอร์ `release`
6. ตรวจว่า GitHub แสดงโครงสร้าง Source Code โดยตรง ไม่ใช่มีเพียงไฟล์ ZIP หรือ .class
7. บน Notebook ดาวน์โหลด Source Code เป็น ZIP แตกไฟล์ แล้วเปิด README เพื่อ Build และ Run

## กรณีใช้ Git CLI

เมื่อมี Git และสร้าง Remote Repository ว่างไว้แล้ว ใช้คำสั่งต่อไปนี้ในโฟลเดอร์โปรเจกต์ แทน URL ด้วย Repository ของตนเอง:

```powershell
git init
git add .
git commit -m "Create Type Strike Java OOP game"
git branch -M main
git remote add origin https://github.com/YOUR-USERNAME/type-strike.git
git push -u origin main
```

ตั้งค่า Git user.name และ user.email ของตนเองหาก Git ขอ และเข้าสู่ระบบด้วยขั้นตอนของ GitHub ไม่ใส่รหัสผ่าน Token หรือข้อมูลลับใน Source Code

รายงานฉบับเต็มให้อัปโหลด Google Drive ส่วนตัวและตั้งสิทธิ์ตามที่อาจารย์กำหนด ตรวจลิงก์ด้วยบัญชีที่ใช้ตรวจจริงก่อนส่ง ไม่จำเป็นต้องเผยแพร่ข้อมูลนักศึกษาหรือเอกสารส่วนตัวใน Repository สาธารณะ
