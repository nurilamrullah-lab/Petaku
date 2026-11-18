# Petaku (GeoShift) - Tactical Location Simulator for Android

Aplikasi simulator dan manipulasi lokasi GPS stealth (anti-deteksi) presisi tinggi untuk perangkat Android, dirancang khusus untuk optimasi hardware chipset Qualcomm Snapdragon / GNSS terhubung antena eksternal (diuji pada ASUS Zenfone Max Pro M2 / Android 16 LineageOS).

---

## Alur Kerja Aplikasi

Navigasi bawah terdiri dari 6 menu: **Peta · Cari · Rute · Satelit · Riwayat · Atur**. Peran layar dipisah dengan tegas:

| Kebutuhan | Menu | Isi |
|---|---|---|
| Mock lokasi **diam** di satu titik | **Peta** (Home) | Pilih titik di peta / koordinat, lalu aktifkan atau hentikan mock. Tanpa slider kecepatan dan tanpa joystick. |
| Simulasi **bergerak** | **Rute** | Mode Rute (asal → titik singgah → tujuan) atau mode Joystick, pengaturan kecepatan km/j, dan kontrol perjalanan. |

1. **Mock statis** — buka *Peta*, ketuk/tekan lama peta atau isi koordinat, lalu tekan *Aktifkan Mock Lokasi*. Posisi tetap di titik tersebut sampai dihentikan.
2. **Simulasi rute** — buka *Rute*, tentukan asal dan tujuan (opsional: tambah titik singgah), atur kecepatan, lalu *Mulai Perjalanan*. Perjalanan bisa *Jeda*, *Lanjutkan*, atau *Hentikan*.
3. **Joystick** — di *Rute*, pilih mode *Joystick* lalu *Mulai Joystick* untuk menggeser posisi secara manual dari titik asal.
4. Saat dihentikan, posisi dikembalikan ke GPS asli HP.

---

## Fitur Utama

- **Peta Interaktif Layar Penuh (Tactical Map HUD)**:
  - Antarmuka peta interaktif berbasis OpenStreetMap / Google Maps (Roadmap, Satellite, Hybrid, Terrain).
  - Rel kontrol ringkas di sisi kanan: Zoom In (+), Zoom Out (-), **satu** tombol GPS HP asli (hanya menggeser kamera, pin target tidak ikut berubah), tombol fokus, dan pemilih layer peta.
  - Di *Peta*, tombol fokus pin hanya muncul ketika pin target berada di luar layar. Di *Rute*, tombol fokus menampilkan seluruh rute atau mengikuti posisi simulasi.
  - Floating HUD telemetri satelit real-time (*In-Use / In-View SATS*), rasio SNR, dan status lock GNSS.
- **Simulasi Rute & Pergerakan (menu Rute)**:
  - Penentuan titik asal (A), tujuan (B), dan titik singgah langsung dari peta atau input alamat.
  - Rute jalan raya otomatis menggunakan OpenStreetMap OSRM Routing Engine, digabung per segmen asal → singgah → tujuan.
  - Kecepatan dalam **km/j** dengan preset kendaraan (Jalan 5, Sepeda 18, Motor 40, Mobil 60 km/j) dan slider 1–150 km/j.
  - Kontrol perjalanan Mulai / Jeda / Lanjutkan / Hentikan, estimasi jarak & durasi, serta marker animasi bergerak.
  - Mode Joystick untuk pergerakan manual.
- **Notifikasi yang Tidak Mengganggu**:
  - Saat mock aktif, notifikasi pemberitahuan muncul **sekali** lalu otomatis hilang dalam 4 detik (bisa juga di-swipe).
  - Notifikasi wajib service latar belakang dibuat senyap (prioritas minimum, tidak ongoing) dan tidak diperbarui setiap perpindahan koordinat, sehingga mock tetap berjalan stabil di background.
- **Stealth & Anti-Deteksi**:
  - Algoritma *Atmospheric Multipath Jitter* (fluktuasi akurasi ±1.5m) untuk mencegah deteksi mock location oleh aplikasi ride-hailing / anti-fraud.
  - Injeksi FusedLocationProvider dan Mock Location Provider tingkat sistem.
  - Broadcast Receiver persistensi otomatis saat boot (`RECEIVE_BOOT_COMPLETED`).
- **Pencarian Lokasi Cerdas (Dual Engine)**:
  - Pencarian alamat taktis via Native Android Geocoder + Nominatim API + Parser koordinat langsung (Latitude, Longitude).
- **Riwayat Lokasi & Ekspor GPX**:
  - Database lokal SQLite untuk penyimpanan titik favorit dan riwayat simulasi.
  - Ekspor rute ke format standar GPX 1.1 ke direktori `/sdcard/Download/`.
- **Sistem Tema Antarmuka Dinamis (8 Varian Visual)**:
  1. *Modern Material 3* (Royal Blue & Slate khas Google Material You)
  2. *Dark OLED* (Pitch Black murni hemat baterai & High Voltage Cyan)
  3. *Glassmorphism 2.0* (Efek blur kaca es translucent & Ice Cyan Glow)
  4. *Aurora Gradient* (Gradasi kosmik Neon Fuchsia, Cosmic Violet & Nebula Teal)
  5. *Neo-Minimal* (Gaya monokrom Swiss, Obsidian Carbon & Pure Snow White)
  6. *Soft Neumorphism* (Warm Matte Charcoal dengan tombol lembut timbul)
  7. *CyberTech* (Futuristik Matrix Neon Green & Cyber Gold)
  8. *Dynamic Color (Monet)* (Adaptif mengikuti palet wallpaper Android 12+)

---

## Arsitektur & Teknologi

- **Bahasa**: Kotlin (Jetpack Compose, Material 3)
- **Kompatibilitas**: Android 8.0 (API 26) - Android 16 (API 36 / Vanilla Ice Cream)
- **Engine Peta**: osmdroid dengan Google Tile Providers
- **Database**: SQLite lokal
- **Routing**: OSRM (Open Source Routing Machine) API

---

## Build & Instalasi

```bash
# Kompilasi Debug / Release APK
./gradlew assembleDebug
./gradlew assembleRelease

# Install ke perangkat via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Setelah instalasi, jadikan Petaku sebagai **aplikasi lokasi palsu** di *Opsi Pengembang → Pilih aplikasi lokasi palsu*, lalu izinkan akses lokasi dan notifikasi.

Telah diuji pada perangkat fisik **ASUS Zenfone Max Pro M2 (Android 16, LineageOS)**.

---
*Pengembangan & Modifikasi oleh Nuril Amrullah (Bang Nuril)*
