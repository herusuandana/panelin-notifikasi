# Privasi Panelin Notifikasi

Berlaku hingga versi 1.4.0. Diperbarui: 28 Oktober 2026.

Aplikasi dipasang di HP pemilik/admin website Panelin, bukan pelanggan yang deposit. Pemilik perangkat memilih sumber notifikasi dan website penerima.

## Data yang diproses

Akses notifikasi Android memberi akses luas di tingkat sistem. Aplikasi memeriksa package yang dipilih dan aktif **sebelum membaca isi notifikasi**, lalu menyaring pembayaran masuk secara lokal. Filter tidak menjamin semua format atau data sensitif dikenali. Jangan pilih aplikasi pribadi yang tidak diperlukan. Android juga dapat menutupi notifikasi sensitif.

Data mencakup package/nama aplikasi, judul, teks, teks panjang, identitas kejadian untuk deduplikasi, waktu pencatatan, tujuan webhook, token, status, dan jumlah percobaan kirim. Daftar aplikasi launcher hanya dipakai lokal untuk memilih sumber, bukan daftar seluruh package sistem.

## Pengiriman

Package, judul, teks, teks panjang, dan token penerima dikirim lewat HTTPS langsung ke URL website pilihan client. Tidak ada endpoint/token bawaan, server pengumpul Panelin, analitik, iklan, atau penjualan data.

Website penerima mengatur penyimpanan, log, dan akses admin sendiri. Aplikasi tidak bisa menghapus data yang sudah diterima website. Unduhan APK mengikuti kebijakan GitHub, terpisah dari pemrosesan notifikasi.

## Penyimpanan perangkat

Konfigurasi sensitif dan payload antrean/riwayat dienkripsi dengan AES-GCM dan kunci Android Keystore. Backup cloud dan transfer data aplikasi dinonaktifkan. Ini tidak menjamin keamanan pada HP root, malware, atau sistem terkompromi. Lindungi perangkat dan layar kuncinya.

Antrean tertunda/ditahan tetap disimpan tanpa batas retensi otomatis agar pembayaran tidak hilang diam-diam. Hapus riwayat hanya menghapus catatan terminal, bukan penanda deduplikasi pencegah replay. Hapus data aplikasi atau uninstall menghapus konfigurasi dan antrean lokal yang mungkin tidak bisa dipulihkan. Lakukan rekonsiliasi lebih dulu.

Diagnostik lokal menyimpan maksimal 2000 langkah terenkripsi: alasan notifikasi dilewati, waktu dan tujuan percobaan, durasi, HTTP, serta field respons `status`/`outcome` yang diizinkan. Isi notifikasi yang dilewati, token, header, dan respons mentah tidak disimpan dalam jejak. Riwayat diagnostik bisa terpangkas tanpa menghapus antrean atau penanda duplikat.

## Izin dan akses

- Akses notifikasi melalui persetujuan di pengaturan Android.
- Notifikasi status pada Android 13 ke atas.
- Internet/status jaringan untuk pengiriman dan retry.
- Visibilitas aplikasi launcher untuk memilih sumber.

Aplikasi tidak meminta PIN/password/OTP dompet, SMS, kontak, lokasi, mikrofon, Accessibility, Device Admin, root, atau akses seluruh penyimpanan. Aplikasi tidak login atau mengendalikan aplikasi pembayaran.

## Kendali dan persetujuan

Anda bisa menjeda pemantauan, menonaktifkan sumber, menghapus konfigurasi/riwayat terminal, dan mencabut akses notifikasi lewat Android. Permintaan yang sedang berjalan mungkin selesai. Data yang sudah dikirim tidak bisa ditarik kembali.

Perubahan endpoint/token atau penghapusan tujuan menahan antrean lama, bukan mengalihkannya. Jika HP hilang atau token bocor, rotasi token di website dan amankan perangkat.

Pahami data dan penerimanya sebelum menyetujui sumber. Akses notifikasi bukan bukti keaslian transaksi. Pengelola website/rekening tetap harus memeriksa mutasi dan kejadian meragukan.

## Bantuan

Hubungi pengelola Panelin Anda. Jangan sertakan token, data pelanggan, atau notifikasi privat di [GitHub Issues](https://github.com/herusuandana/panelin-notifikasi/issues). Lihat [panduan pemasangan](README.md).
