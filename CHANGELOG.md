# Catatan pembaruan

APK dan checksum: [GitHub Releases](https://github.com/herusuandana/panelin-notifikasi/releases). Versi mengikuti `versionName` dan `versionCode`. Catatan juga tersedia di Pengaturan → Catatan pembaruan.

## 1.4.0 — 2026-10-28

versionCode: 6 · Prerelease uji terbatas, QA belum lengkap

- Riwayat menampilkan notifikasi yang dilewati di HP dan alasannya.
- Jejak tiap percobaan webhook: tujuan, waktu, durasi, HTTP, dan hasil respons.
- Respons disaring. Token, OTP/PIN, dan respons mentah tidak disimpan dalam jejak.
- Maksimal 2000 langkah diagnostik, terpisah dari antrean dan pengaman duplikat.

## 1.3.0 — 2026-10-28

versionCode: 5 · Build lokal, belum dipublikasikan

- Tampilan ala iOS, navigasi adaptif HP/tablet, dan mode terang/gelap.
- Teks aplikasi, panduan, dan dokumentasi lebih singkat.
- Antrean, webhook, enkripsi, dan perlindungan token dipertahankan.
- Hasil pengujian dicatat di [QA](QA-RESULTS.md).

## 1.2.0 — 2026-10-27

**versionCode: 4 · Uji terbatas**

- Migrasi Android Views ke Jetpack Compose tanpa JavaScript atau WebView.
- Empat halaman reaktif, animasi navigasi/toggle, bottom sheet, ikon custom, pencarian aplikasi, dan rail layar lebar.
- Gagal baca penyimpanan tampil sebagai status tidak diketahui. Baca/simpan berjalan di luar thread UI.
- Token tetap tersembunyi dan tidak masuk saved state. Autofill dimatikan, sheet sensitif memakai FLAG_SECURE.
- Listener, enkripsi, antrean, parser, webhook, dan signing tetap sama. Tidak ada transaksi uji yang dikirim.
- Crash Android 15 belum terbukti selesai. Uji HP fisik masih diperlukan.

## 1.1.0 — 2026-10-27

**versionCode: 3 · Kandidat uji lokal**

- Desain ulang Beranda, Riwayat, Aplikasi, dan Pengaturan, termasuk ikon, tipografi, dan navigasi.
- Kontrol Panelin untuk tombol, input, selector sheet, toggle, checkbox, dialog, dan pesan aplikasi, tanpa chrome kontrol bawaan.
- Penyesuaian layar sempit, landscape, tablet, teks besar, keyboard, dan area aman sistem.
- Semantik aksesibilitas tetap dipertahankan. Semua dialog memakai FLAG_SECURE.
- Penyimpanan, antrean, webhook, parser, dan saldo tidak berubah. Izin/pengaturan Android tetap memakai UI sistem.

## 1.0.1 — 2026-10-27

**versionCode: 2 · Uji terbatas**

- Memperbaiki urutan akses controller system bar saat startup Android 11 ke atas, termasuk Android 15. Konfigurasi dijalankan setelah content view terpasang.
- Menambahkan guard startup dan tes cold start, tanpa mengubah izin atau alur pembayaran.
- Package dan sertifikat tetap sama untuk pembaruan tanpa uninstall.

## 1.0.0 — 2026-10-27

**versionCode: 1 · Uji terbatas**

- Rilis awal untuk pemilik/admin website Panelin, dengan pencarian aplikasi pembayaran dan kendali tiap sumber.
- URL HTTPS dan token per client, tanpa server perantara bawaan.
- NotificationListenerService dan antrean persisten terenkripsi Android Keystore. Retry terbatas, antrean lama ditahan jika endpoint/token berubah.
- Riwayat membedakan kredit, duplikat, diabaikan, tidak cocok, ambigu, dan review.
- Panduan privasi, izin notifikasi, baterai/autostart OEM, logo, versi, dan changelog.
- APK memakai sertifikat release tetap. Backup data aplikasi dinonaktifkan.

## Batas dan pembaruan

Pembayaran DANA hingga kredit saldo dan ketahanan background pada HP client belum diuji end-to-end. Baca [hasil QA](QA-RESULTS.md) dan ikuti [panduan uji](TESTING.md) sebelum dipakai.

Belum ada updater otomatis. Unduh APK resmi yang lebih baru dan pasang di atas versi lama dengan package serta sertifikat sama. Jangan uninstall jika masih ada antrean. Rilis berikutnya wajib menaikkan `versionCode`.
