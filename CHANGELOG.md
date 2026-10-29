# Catatan pembaruan

APK dan checksum: [GitHub Releases](https://github.com/herusuandana/panelin-notifikasi/releases). Catatan juga ada di Pengaturan → Catatan pembaruan.

## 1.6.1 — 2026-10-29

versionCode: 9 · Rilis normal

- Memperbaiki siklus swipe refresh agar status memuat diterapkan sebelum pembacaan data selesai.
- Skeleton beranimasi saat pemuatan awal dan refresh manual. Refresh membaca ulang data lokal, bukan mengirim ulang pembayaran.
- Batas pengujian perangkat tetap dicatat di [hasil QA](QA-RESULTS.md).

## 1.6.0 — 2026-10-28

versionCode: 8 · Prerelease uji terbatas, QA runtime belum lengkap

- Cek rilis setiap dibuka/kembali ke depan. Cek otomatis diam jika tidak ada versi baru atau jaringan gagal. Bawaan stabil, rilis uji opsional.
- Unduh atas pilihan pengguna, periksa SHA-256/package/versi/signature, lalu konfirmasi lewat Android. APK privat dibagikan melalui FileProvider khusus pembaruan.
- Empat halaman native ala macOS lebih ringkas, tarik untuk memuat ulang, skeleton, pilihan khusus, dan ikon package sumber di Riwayat. Backend pembayaran tidak berubah.

## 1.5.0 — 2026-10-28

versionCode: 7 · Prerelease uji terbatas

- Beranda biru–toska dengan ringkasan pemantauan dan pintasan perangkat.
- Kartu membulat, ikon berwarna, dan navigasi kapsul. Antrean, enkripsi, dan jejak webhook tetap sama.

## 1.4.0 — 2026-10-28

versionCode: 6 · Prerelease uji terbatas, QA belum lengkap

- Riwayat mencatat alasan notifikasi dilewati dan tiap percobaan webhook: tujuan, waktu, durasi, HTTP, dan hasil respons.
- Maksimal 2000 langkah diagnostik terenkripsi, terpisah dari antrean/deduplikasi. Jejak tidak menyimpan token, OTP/PIN, atau respons mentah.

## 1.3.0 — 2026-10-28

versionCode: 5 · Build lokal, belum dipublikasikan

- Tampilan ala iOS, navigasi adaptif HP/tablet, dan mode terang/gelap.
- Teks dan panduan lebih singkat. Antrean, webhook, enkripsi, dan perlindungan token tetap sama.

## 1.2.0 — 2026-10-27

versionCode: 4 · Uji terbatas

- Migrasi ke Jetpack Compose tanpa JavaScript/WebView, dengan animasi, sheet, pencarian, dan navigasi adaptif.
- Penyimpanan berjalan di luar thread UI. Gagal baca tampil sebagai status tidak diketahui. Token tidak masuk saved state, autofill dimatikan, sheet sensitif memakai FLAG_SECURE.
- Listener, antrean, webhook, parser, dan signing tetap sama. Tidak ada transaksi uji. Crash Android 15 dan HP fisik belum terverifikasi.

## 1.1.0 — 2026-10-27

versionCode: 3 · Kandidat uji lokal

- Desain ulang empat halaman dan kontrol Panelin untuk layar sempit, landscape, tablet, teks besar, dan keyboard.
- Semantik aksesibilitas dipertahankan, dialog memakai FLAG_SECURE. Izin/pengaturan Android tetap memakai UI sistem.
- Penyimpanan, antrean, webhook, parser, dan saldo tidak berubah. Penyesuaian layout bukan bukti semua ukuran sudah diuji.

## 1.0.1 — 2026-10-27

versionCode: 2 · Uji terbatas

- Memperbaiki urutan startup system bar Android 11+, termasuk Android 15, setelah content view terpasang.
- Menambahkan guard startup dan tes cold start. Package/sertifikat tetap sama untuk pembaruan tanpa uninstall.
- Crash HP pelapor dan runtime Android 15 belum terverifikasi. Izin dan alur pembayaran tidak berubah.

## 1.0.0 — 2026-10-27

versionCode: 1 · Uji terbatas

- Rilis awal untuk admin Panelin: pilih sumber, URL HTTPS, dan token per client, tanpa server perantara.
- Listener dan antrean terenkripsi Android Keystore dengan retry terbatas. Perubahan URL/token menahan antrean lama. Riwayat membedakan kredit, duplikat, diabaikan, tidak cocok, ambigu, dan review.
- Panduan privasi/izin/baterai/autostart, versi, dan changelog. Sertifikat release tetap, backup aplikasi dimatikan.

## Batas

Pembayaran hingga kredit saldo dan ketahanan background di HP client belum diuji end-to-end. Lihat [hasil QA](QA-RESULTS.md) dan [panduan uji](TESTING.md). HTTP 200 bukan bukti kredit saldo. Jangan uninstall jika masih ada antrean. Rilis berikutnya wajib menaikkan `versionCode` dan memakai sertifikat resmi yang sama.
