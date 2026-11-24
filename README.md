# Otacix by Oscar Mandala

Aplikasi musik Android (Kotlin + Jetpack Compose) bertema **liquid glass ala iOS 26**, dominan **biru & kuning** sesuai logo.
Pemutaran berjalan di **latar belakang** (layanan Media3 + notifikasi pemutar), dan tampilan/fitur mengikuti web app Ocalix (file `c2.zip`).

## Cara membuka & menjalankan
1. Install **Android Studio** (Ladybug 2024.2 atau lebih baru). Buka folder `Otacix` ini → tunggu *Gradle Sync* (butuh internet, otomatis memakai Gradle 8.9).
2. Bila diminta, install **Android SDK 35**.
3. Colok HP (USB debugging) atau pakai emulator → tombol **Run ▶**.
4. APK siap pasang: menu **Build ▸ Build APK(s)**.

Minimal Android 7.0 (API 24).

## Build otomatis di GitHub Actions
1. Upload **isi** folder `Otacix` (bukan folder luarnya) ke repo GitHub, di branch `main`.
2. Buka tab **Actions** -> workflow **Build APK** berjalan otomatis (atau klik *Run workflow*).
3. Setelah selesai (sekitar 5-10 menit), unduh APK dari bagian **Artifacts** -> `Otacix-debug-apk`.

APK ini versi *debug* (bisa langsung dipasang di HP). Workflow ada di `.github/workflows/build.yml`.

## Fitur
- **Splash screen**: splash sistem + splash animasi (logo pegas, "OTACIX", "by Oscar Mandala").
- **Beranda**: sapaan, pilihan cepat, streak harian, suasana hati (mood), rak lagu, Poster Studio.
- **Cari**: pencarian YouTube (InnerTube), riwayat pencarian, cadangan YouTube Music + Piped; iTunes hanya untuk metadata (audio tetap penuh).
- **Pustaka**: lagu disukai, playlist (buat/ganti nama/hapus), terakhir diputar.
- **Pemutar penuh**: seek, acak, ulang, antrian (lompat/hapus), kecepatan 0.75×–1.5×, **timer tidur dengan fade-out** (menit atau akhir lagu).
- **Latar belakang**: tetap memutar saat layar mati / aplikasi ditutup dari layar, kontrol di notifikasi & layar kunci.
- **Cache audio**: lagu yang pernah diputar bisa diputar ulang tanpa internet.
- **Poster Studio**: playlist → poster liquid glass (**Kartu** pemutar miring / Grid 3×3 / Tumpuk), simpan ke Galeri atau bagikan.
- **Lirik**: tombol lirik di pemutar penuh (sumber LRCLIB), baris yang sedang dinyanyikan disorot dan bisa diketuk untuk loncat.
- **Dukung developer**: popup QRIS setelah splash (bisa dibuka lagi di tab Kamu), QRIS bisa disimpan ke Galeri.
- Semua ikon memakai vektor SVG (tanpa emoji).
- **Cadangkan / Pulihkan** data (JSON).

### Mode performa (otomatis)
| | **Hemat** (HP spek rendah) | **Penuh** (HP spek atas) |
|---|---|---|
| Latar | gradien biru-kuning statis | aurora bergerak + warna mengikuti sampul lagu |
| Kaca | translusen + tepi berkilau | + **kilau spekular mengikuti kemiringan HP** (sensor gravitasi) |
| Pemutar | latar aurora | sampul diburamkan (Android 12+) |
| Cache | gambar 60 MB, audio 128 MB, RGB565 | gambar 150 MB, audio 512 MB |

Terdeteksi otomatis (RAM ≥ 5,5 GB, ≥ 8 inti, Android 10+). Bisa dipaksa di **Kamu ▸ Performa**.

## Struktur kode (mirip pola Metrolist)
```
app/src/main/java/id/oscarmandala/otacix/
  OtacixApp.kt, MainActivity.kt
  data/    Models, Store (penyimpanan lokal), Yt (YouTube/Piped/iTunes), Repo
  player/  PlaybackService (Media3 ExoPlayer + MediaSession), PlayerController, Mappers
  ui/      Theme, Glass (efek liquid glass, tilt, aurora), Icons, Components, Dialogs,
           Root, Home, Search, Library, Profile, Player, Poster, Splash
  util/    Device (deteksi spek), PosterRenderer
```

## Catatan penting
- **Bukan salinan kode Metrolist.** Di lingkungan pembuatan, akses GitHub diblokir sehingga repo Metrolist tidak bisa di-clone. Kode ditulis ulang dari nol dengan arsitektur setara (Compose + Media3 + layanan latar belakang). Metrolist berlisensi **GPL-3.0**: bila kamu menyalin kode dari sana ke proyek ini, proyek turunannya wajib dirilis dengan GPL-3.0 juga.
- **Belum dikompilasi.** Proyek ditulis tanpa Android SDK/Gradle, jadi belum pernah di-build. Bila ada error saat Sync/Build, kirim pesan error-nya agar bisa diperbaiki.
- Sumber lagu memakai **API YouTube tidak resmi** (seperti web Ocalix dan Metrolist). Bisa berhenti berfungsi kapan saja dan bisa melanggar ketentuan layanan YouTube; cocok untuk belajar/pemakaian pribadi, bukan untuk dirilis ke Play Store.
- Bila lagu gagal diputar, isi `Yt.customPiped` (di `data/Yt.kt`) dengan alamat instance Piped milikmu sendiri.
- `gradle-wrapper.jar` tidak disertakan; Android Studio mengunduh Gradle sendiri lewat `gradle-wrapper.properties`.


## Pemutaran (v3)
- URL audio: NewPipeExtractor (jalur utama), lalu klien YouTube langsung dan Piped sebagai cadangan.
- Audio diunduh per potongan 1 MiB (`player/ChunkedDataSource.kt`) memakai teknik pemutar Metrolist,
  supaya tidak ditolak/terputus oleh server googlevideo.
- Bila lagu gagal diputar, toast menampilkan alasannya.
