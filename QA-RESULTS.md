# Hasil pengujian

## v1.0.1 — perbaikan startup (versionCode 2)

- Memindahkan akses `window.insetsController` setelah `setContentView(root)`. Implementasi Android PhoneWindow mengakses decor secara langsung di dalam getter; safe-call Kotlin tidak melindungi exception dari getter sebelum decor tersedia.
- Guard `python3 scripts/check-startup.py` gagal pada urutan lama dan lulus setelah perbaikan. Guard ini memeriksa urutan source, bukan runtime Android.
- Tes instrumentation `StartupTest` ditambahkan untuk membuka Activity dan memeriksa dashboard; APK tes berhasil dikompilasi.
- Build release/debug/instrumentation berhasil, lima unit test lulus, lint release tanpa error.
- Sertifikat release tetap sama dengan v1.0.0; versionCode naik dari 1 menjadi 2. APK dapat digunakan sebagai pembaruan tanpa uninstall.
- Runtime Android 15 **belum terverifikasi**: emulator API 35 software tanpa KVM belum menyelesaikan boot. Log crash HP pelapor belum diterima, sehingga hubungan error spesifik di HP dengan defect startup ini belum dikonfirmasi lewat stack trace.
- Belum ada perubahan izin, kontrak webhook, atau pengujian pembayaran produksi.

SHA-256 APK v1.0.1:

```text
7851eb8f552288b0906fa2bdf925b8e02bbdbdcb81059bb5c2bad762de8395c4
```

## Arsip hasil v1.0.0

Tanggal pengujian: 27 Oktober 2026. Package `id.panelin.notifikasi`, versionCode `1`, minimum Android 8.0/API 26, target SDK 35.

**Kesimpulan: APK signed untuk uji terbatas, bukan pernyataan siap produksi.** Hasil di bawah berasal dari build lokal dan emulator; tidak ada pembayaran nyata atau webhook pembayaran yang dikirim ke website client produksi.

## Hasil terverifikasi

| Pemeriksaan | Hasil |
| --- | --- |
| Gradle unit test | 5 tes, 0 failure/error |
| Lint release | 0 error, 21 warning |
| Build release, debug, dan instrumentation | Berhasil |
| Signature APK release | Valid, APK Signature Scheme v2; bukan debug key |
| Instalasi APK release | Berhasil; package/versi sesuai dan tidak debuggable |
| Penyimpanan pada Android 8 | 2 tes StoreSecurityTest lulus |
| Antrean saat dijeda, hapus riwayat, dan perubahan tujuan | 1 tes QueueBehaviorTest lulus |
| Status tanpa akses listener | 1 tes StatusNotificationTest lulus |
| JSON/outcome worker dengan transport in-memory | 1 tes WorkerContractTest lulus |
| Navigasi langsung APK release | Keempat tab diverifikasi dengan judul konten |
| Versi dan changelog | Scroll Pengaturan, versi 1.0.0 (1), dan dialog changelog berhasil |
| Arsip source | ZIP valid; keystore, kredensial lokal, dan local.properties tidak disertakan |

Tes penyimpanan menggunakan SQLite dan Keystore Android nyata. Token/endpoint tidak ditemukan plaintext dalam database/preferences yang diperiksa; ini bukan audit forensik menyeluruh. WorkerContractTest menjalankan worker dan JSON sebenarnya tetapi mengganti transport HTTPS dengan koneksi in-memory, sehingga bukan bukti TLS atau integrasi Laravel end-to-end.

Screenshot halaman yang diperiksa memperlihatkan logo, teks, navigasi, dan dialog changelog terbaca. Contoh tersedia di [Beranda](docs/images/beranda.png) dan [Riwayat](docs/images/riwayat.png). Detail pengujian tersedia di [TESTING.md](TESTING.md).

Warning lint mencakup versi dependensi lebih baru, alokasi saat menggambar ikon, resource launcher, string UI Indonesia, dan penggunaan commit preferensi. Tidak dipasang baseline untuk menyembunyikan error.

## Temuan dan batas emulator

- Android 8 menolak PRAGMA `secure_delete=ON` melalui `execSQL` karena menghasilkan baris. Pemanggilan diperbaiki menjadi `rawQuery` dengan pemeriksaan hasil; kedua tes penyimpanan yang sebelumnya gagal kemudian lulus di emulator yang sama.
- ClientSmokeTest instrumentation tidak lulus: dialog sistem “Process system isn't responding” menutupi UI. Emulator software tanpa KVM juga mengalami timeout dan ANR System UI. Kegagalan ini tetap dicatat, bukan diubah menjadi lulus.
- Pemeriksaan terpisah pada APK release yang terpasang kemudian berhasil berpindah ke empat tab dan membuka changelog tanpa dialog sistem. Ini membuktikan navigasi dasar pada artefak release, bukan kelulusan seluruh instrumentation suite atau semua perangkat.

## Belum diverifikasi

- Notifikasi DANA/aplikasi pembayaran nyata → webhook Laravel → kredit saldo.
- Listener, jaringan offline/pulih, reboot, Doze, dan pembatasan vendor pada HP fisik.
- Font besar, keyboard, rotasi, Android terbaru, dan berbagai merek HP secara menyeluruh.
- Pembaruan APK dengan versionCode lebih tinggi tanpa kehilangan konfigurasi/antrean.
- Semua format DANA pribadi atau sumber pembayaran lain.

## Identitas APK yang diuji

APK: `panelin-notifikasi-1.0.0-release.apk`

SHA-256 APK:

```text
52be0707147dc58225f399b459eb623be862251dfff32573c44e46666139db8d
```

Sertifikat release: `CN=Panelin Android Release, O=Panelin, C=ID`

SHA-256 sertifikat:

```text
70f30880391bd102c4392f1ef489fc58bd6f90d15a405a1ecbd068b22fe634e8
```

Cocokkan file `.sha256` dan fingerprint ketika menerima APK. Sertifikat yang sama diperlukan untuk pembaruan resmi.
