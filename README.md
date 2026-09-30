# Petaku (GeoShift) - Tactical Location Simulator for Android

Aplikasi simulator dan manipulasi lokasi GPS stealth (anti-deteksi) presisi tinggi untuk perangkat Android, dirancang khusus untuk optimasi hardware chipset Qualcomm Snapdragon / GNSS terhubung antena eksternal (ASUS ZenFone Max Pro M1 / Android 16).

---

## Fitur Utama

- **Peta Interaktif Layar Penuh (Tactical Map HUD)**:
  - Antarmuka peta interaktif berbasis OpenStreetMap / Google Maps (Roadmap, Satellite, Hybrid, Terrain).
  - Kontrol presisi di sisi kanan tengah: Zoom In (+), Zoom Out (-), Fokus Pin Target, Fokus GPS HP Asli, dan Pemilih Layer Peta.
  - Floating HUD telemetri satelit real-time (*In-Use / In-View SATS*), rasio SNR, dan status lock GNSS.
- **Simulasi Rute Bergaya inDrive**:
  - Penentuan titik jemput (A) dan tujuan (B) langsung dari peta atau input alamat.
  - Rute jalan raya otomatis menggunakan OpenStreetMap OSRM Routing Engine.
  - Estimasi jarak, durasi, dan kecepatan simulasi perjalanan (1x hingga 5x) dengan marker animasi bergerak.
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
# Kompilasi Release APK
./gradlew assembleRelease

# Install ke Perangkat via ADB
adb install -r GeoShift-release.apk
```

---
*Pengembangan & Modifikasi oleh Nuril Amrullah (Bang Nuril)*
