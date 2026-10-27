# Catatan pembaruan

APK resmi dan checksum tersedia di [GitHub Releases](https://github.com/herusuandana/panelin-notifikasi/releases). Versi di aplikasi mengikuti `versionName` dan `versionCode`; catatan yang sama tersedia melalui Pengaturan → Yang baru di versi ini.

## 1.2.0 — 2026-10-27

**versionCode: 4 · Status: uji terbatas — Jetpack Compose**

- Mengganti UI Android Views manual dengan Jetpack Compose; tanpa JavaScript atau WebView.
- Empat halaman reaktif, animasi navigasi/toggle, bottom sheet, ikon custom, pencarian aplikasi, dan rail untuk layar lebar.
- Status penyimpanan gagal ditampilkan sebagai tidak diketahui; operasi simpan dan baca dilakukan di luar thread UI.
- Token tetap tersembunyi, tidak masuk saved state, autofill dimatikan; sheet sensitif dilindungi FLAG_SECURE.
- Layanan notifikasi, penyimpanan terenkripsi, antrean, parser, webhook, dan signing tetap sama. Tidak mengirim transaksi uji.
- Migrasi UI tidak membuktikan crash Android 15 sudah terselesaikan; pengujian HP fisik tetap diperlukan.

## 1.1.0 — 2026-10-27

**versionCode: 3 · Status: kandidat uji lokal**

- Desain ulang Beranda, Riwayat, Aplikasi, dan Pengaturan dengan hierarki, ikon, tipografi, serta navigasi baru.
- Komponen visual Panelin untuk tombol, input, selector berbentuk sheet, toggle, checkbox, dialog, dan pesan dalam aplikasi; tidak menggunakan chrome kontrol bawaan.
- Penyesuaian layar sempit, landscape, tablet, teks besar, keyboard, dan area aman sistem.
- Semantik aksesibilitas kontrol tetap dipertahankan; seluruh dialog dilindungi FLAG_SECURE.
- Tidak mengubah penyimpanan, antrean, webhook, parser, atau saldo. Izin dan pengaturan milik Android tetap memakai UI sistem.

## 1.0.1 — 2026-10-27

**versionCode: 2 · Status: uji terbatas**

- Memperbaiki akses controller system bar sebelum window decor tersedia saat startup pada Android 11 ke atas, termasuk Android 15.
- Memindahkan konfigurasi system bar setelah pemasangan content view tanpa mengubah izin atau alur pembayaran.
- Menambahkan guard urutan startup dan tes cold start Android.
- Tetap memakai package dan sertifikat release yang sama agar dapat diperbarui tanpa uninstall.

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
