# Privasi Panelin Notifikasi

Berlaku hingga versi 1.6.0. Diperbarui: 28 Oktober 2026.

Aplikasi ini untuk HP pemilik/admin website Panelin, bukan pelanggan yang deposit. Pemilik perangkat memilih sumber dan penerima data.

## Data dan pengiriman

Izin notifikasi Android mencakup semua notifikasi. Aplikasi memeriksa package terpilih dan aktif sebelum membaca isinya, lalu menyaring pembayaran masuk di HP. Filter tidak mengenali semua format/data sensitif. Pilih hanya aplikasi yang diperlukan. Android juga bisa menutupi isi sensitif.

Data meliputi package/nama aplikasi, judul, teks, teks panjang, identitas deduplikasi, waktu, URL, token, status, dan jumlah percobaan. Daftar aplikasi launcher hanya dipakai lokal untuk memilih sumber, bukan inventaris seluruh package sistem.

Package, judul, teks, teks panjang, dan token dikirim langsung ke URL HTTPS pilihan Anda. Tidak ada endpoint/token bawaan, server pengumpul Panelin, analitik, iklan, atau penjualan data. Website mengatur penyimpanan, log, dan akses admin. Aplikasi tidak bisa menarik atau menghapus data yang sudah diterima website.

Setiap dibuka atau kembali ke depan, aplikasi mengecek rilis resmi lewat GitHub. GitHub menerima IP dan metadata koneksi, bukan token, daftar sumber, atau isi notifikasi. Cek otomatis diam jika tidak ada rilis baru atau jaringan gagal. Bawaan hanya rilis stabil, rilis uji bersifat pilihan. Unduhan memerlukan pilihan Anda dan pemasangan memerlukan konfirmasi Android.

## Penyimpanan

Konfigurasi sensitif dan payload antrean/riwayat dienkripsi AES-GCM dengan Android Keystore. Backup cloud dan transfer data aplikasi dimatikan. Enkripsi tidak melindungi sepenuhnya dari root, malware, atau sistem terkompromi. Amankan HP dan layar kunci.

Antrean tertunda/ditahan disimpan tanpa batas retensi otomatis agar pembayaran tidak hilang diam-diam. Hapus riwayat hanya menghapus catatan selesai, bukan antrean atau penanda duplikat. Hapus data/uninstall menghapus konfigurasi dan antrean yang mungkin tidak bisa dipulihkan. Rekonsiliasi lebih dulu.

Jejak lokal menyimpan maksimal 2000 langkah terenkripsi: alasan skip, waktu/tujuan percobaan, durasi, HTTP, dan field `status`/`outcome` yang diizinkan. Isi notifikasi yang dilewati, token, header, dan respons mentah tidak disimpan dalam jejak. Pemangkasan jejak tidak menghapus antrean atau penanda duplikat.

APK pembaruan disimpan privat. FileProvider hanya membagikan direktori pembaruan kepada installer, bukan penyimpanan lain. SHA-256, package, versi, dan signature diperiksa sebelum pemasangan.

## Izin dan kendali

Aplikasi meminta akses notifikasi melalui Android, notifikasi status (Android 13+), internet/status jaringan, visibilitas aplikasi launcher, dan izin memasang paket untuk pembaruan yang Anda setujui.

Aplikasi tidak meminta PIN/password/OTP dompet, SMS, kontak, lokasi, mikrofon, Accessibility, Device Admin, root, atau akses seluruh penyimpanan. Aplikasi tidak login atau mengendalikan aplikasi pembayaran.

Pahami sumber, data, dan penerimanya sebelum menyetujui pengiriman. Anda bisa menjeda penerusan, menonaktifkan sumber, menghapus konfigurasi/riwayat selesai, atau mencabut akses notifikasi. Permintaan yang sedang berjalan mungkin selesai.

Mengganti URL/token atau menghapus tujuan menahan antrean lama, bukan mengalihkannya. Jika HP hilang atau token bocor, rotasi token di website. Akses notifikasi bukan bukti keaslian pembayaran. Pengelola tetap harus memeriksa mutasi dan transaksi meragukan.

## Bantuan

Hubungi pengelola Panelin. Jangan bagikan token, data pelanggan, atau notifikasi privat di [GitHub Issues](https://github.com/herusuandana/panelin-notifikasi/issues). Lihat [panduan pemasangan](README.md).
