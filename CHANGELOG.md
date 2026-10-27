# Catatan pembaruan

APK resmi dan checksum tersedia di [GitHub Releases](https://github.com/herusuandana/panelin-notifikasi/releases). Versi di aplikasi mengikuti `versionName` dan `versionCode`; catatan yang sama tersedia melalui Pengaturan → Yang baru di versi ini.

## 1.0.0 — 2026-10-27

**versionCode: 1 · Status: uji terbatas**

- Rilis perdana aplikasi pendamping untuk pemilik/admin website Panelin.
- Pemilihan aplikasi pembayaran terpasang, pencarian, dan aktif/nonaktif per sumber.
- URL webhook HTTPS serta token masing-masing client, tanpa server perantara bawaan.
- Pemantauan melalui NotificationListenerService dan antrean persisten terenkripsi dengan Android Keystore.
- Retry terbatas; antrean lama ditahan bila endpoint/token berubah.
- Riwayat hasil backend yang membedakan kredit, duplikat, diabaikan, tidak cocok, ambigu, dan review.
- Panduan privasi, akses notifikasi, baterai, dan autostart/OEM.
- Logo Panelin, versi, dan changelog di dalam aplikasi.
- APK release bertanda tangan dengan sertifikat tetap; backup data aplikasi dinonaktifkan.

### Batas rilis

Pembayaran DANA nyata sampai kredit saldo dan ketahanan background pada HP client belum diuji end-to-end. Hasil yang sudah terverifikasi serta batas emulator dicatat di [QA-RESULTS.md](QA-RESULTS.md). Ikuti [TESTING.md](TESTING.md) sebelum penggunaan operasional.

### Pembaruan aplikasi

Belum ada updater otomatis. Unduh APK resmi yang lebih baru dari Releases, lalu pasang di atas versi lama dengan package dan sertifikat sama. Jangan uninstall bila masih memiliki antrean tertunda. Perubahan versi berikutnya wajib menaikkan `versionCode`.
