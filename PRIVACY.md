# Kebijakan privasi — Panelin Notifikasi

Berlaku untuk versi 1.0.0. Diperbarui: 27 Oktober 2026.

Aplikasi ini dipasang pada HP pemilik/admin website Panelin, bukan HP pelanggan yang melakukan deposit. Pemilik perangkat menentukan sumber notifikasi dan website penerimanya.

## Data yang diproses

Akses notifikasi Android memberi visibilitas luas di tingkat sistem. Aplikasi memeriksa apakah package sumber dipilih dan diaktifkan **sebelum membaca isi notifikasi**, lalu menerapkan penyaringan lokal untuk pembayaran masuk. Penyaringan bukan jaminan mengenali setiap format atau semua informasi sensitif; jangan pilih aplikasi pribadi yang tidak diperlukan. Android juga dapat menutupi notifikasi sensitif.

Data yang diproses mencakup package/nama aplikasi, judul, teks, teks panjang, identitas kejadian untuk deduplikasi lokal, waktu pencatatan, tujuan webhook, token, serta status dan jumlah percobaan pengiriman. Daftar aplikasi yang memiliki launcher digunakan secara lokal untuk pemilihan sumber; bukan jaminan seluruh package sistem dapat ditampilkan.

## Tujuan pengiriman

Package, judul, teks, teks panjang, dan token penerima dikirim melalui HTTPS langsung ke URL website yang dikonfigurasi client. Tidak ada endpoint/token bawaan, server pengumpul Panelin, analitik, iklan, atau penjualan data di aplikasi ini.

Website penerima memiliki kebijakan penyimpanan, log, dan akses admin sendiri. Aplikasi Android tidak menghapus data yang sudah diterima website. Mengunduh APK dari GitHub mengikuti kebijakan GitHub; pengunduhan tersebut terpisah dari pemrosesan notifikasi oleh aplikasi.

## Penyimpanan pada perangkat

Konfigurasi sensitif dan payload antrean/riwayat dienkripsi menggunakan AES-GCM dengan kunci Android Keystore. Backup cloud dan aturan transfer data aplikasi dinonaktifkan. Perlindungan ini bukan jaminan terhadap HP yang di-root, malware, atau sistem yang dikompromikan. Lindungi perangkat dan layar kuncinya.

Antrean tertunda/ditahan tetap disimpan agar pembayaran tidak hilang diam-diam. Menghapus riwayat hanya menghapus catatan terminal; penanda deduplikasi lokal dipertahankan untuk mencegah replay. Tidak ada batas retensi otomatis yang menghapus antrean belum selesai. Menghapus data aplikasi atau uninstall menghapus konfigurasi/antrean lokal dan dapat menghilangkan kemampuan memulihkannya; jangan lakukan sebelum rekonsiliasi.

## Izin dan akses

- Akses notifikasi, diberikan secara sadar melalui pengaturan Android.
- Izin notifikasi status pada Android 13 atau lebih baru.
- Internet/status jaringan untuk pengiriman dan retry.
- Visibilitas aplikasi launcher untuk memilih sumber.

Aplikasi tidak meminta PIN/password/OTP dompet, SMS, kontak, lokasi, mikrofon, Accessibility, Device Admin, root, atau akses seluruh penyimpanan. Aplikasi tidak membuka sesi login atau mengendalikan aplikasi pembayaran.

## Kendali client

Anda dapat menjeda pemantauan, menonaktifkan sumber, menghapus konfigurasi/riwayat terminal, dan mencabut akses notifikasi melalui Android. Permintaan yang sudah berlangsung mungkin tetap selesai; byte yang sudah dikirim tidak dapat ditarik kembali.

Perubahan endpoint/token atau penghapusan tujuan menahan antrean lama, bukan mengalihkannya ke website baru. Bila HP hilang atau token terpapar, rotasi token pada website dan amankan perangkat.

## Persetujuan dan bantuan

Pemilik perangkat harus memahami data yang dikirim serta penerimanya sebelum mengaktifkan sumber. Akses notifikasi bukan bukti keaslian transaksi; pemeriksaan mutasi dan kejadian meragukan tetap tanggung jawab pengelola website/rekening.

Untuk pertanyaan privasi, hubungi pengelola Panelin Anda. Jika melaporkan masalah melalui [GitHub Issues](https://github.com/herusuandana/panelin-notifikasi/issues), jangan sertakan token, data pelanggan, atau isi notifikasi privat. Panduan pemasangan tersedia di [README](README.md).
