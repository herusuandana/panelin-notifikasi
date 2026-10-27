# Hasil pengujian

## v1.2.0 — Jetpack Compose (versionCode 4, uji terbatas)

- Build signed/debug/instrumentation berhasil pada iterasi migrasi; sertifikat sama. Lima unit test lulus; lint 0 error, 23 warning.
- Runtime API 26 awal: StartupTest (1), ComponentTest (2), RedesignTest (1) lulus. ResponsiveTest gagal karena pencarian item LazyColumn yang belum terkomposisi; tes diperbaiki memakai scroll-to-node dan pengulangan kemudian lulus.
- Emulator sempat terhalang ANR System UI/Process system. Hasil yang terhalang tidak dinilai lulus. Konfigurasi sisa pengujian lama (640×1136/font 1.6) direset ke 720×1280/font 1.0 sebelum suite.
- APK final setelah penyesuaian navigasi teks besar berhasil dibangun. Navigasi empat halaman pada APK signed non-debuggable lulus melalui hierarchy aksesibilitas; preview berasal dari APK ini, bukan mockup.
- APK 1.2.0 berukuran 1.31 MiB (sebelumnya 1.91 MiB); R8 serta resource shrinking aktif. Ukuran APK bukan benchmark pemakaian RAM/CPU.
- Runtime API 26: Startup 1, Component 2, Redesign 1, Responsive 1, EditorSecurity 1, StoreSecurity 2, QueueBehavior 1, StatusNotification 1, WorkerContract 1 lulus (11 tes pada 9 kelas).
- StatusNotificationTest awal gagal pada assertion segera setelah cancel; setelah menunggu acknowledgement NotificationManager maksimal 5 detik, tes ulang lulus (2.764 detik). Tidak ada perubahan kode penerusan. ClientSmokeTest tetap gagal setelah pengulangan: `Error while connecting UiAutomation`. Total terakhir 11 tes lulus, 1 gagal/terhalang; suite keseluruhan tidak dinyatakan lulus. Navigasi APK signed diuji terpisah dan lulus.
- Uji gabungan sebelumnya juga mencatat kegagalan assertion rekreasi Activity dan status; hasil awal tidak diubah menjadi lulus. Rekreasi kini melalui ActivityScenario, dan editor/token/consent lulus. Hasil v1.1.0 tidak berlaku otomatis untuk Compose.
- SHA-256 APK 1.2.0: `2fa6c9783cca3e28ef0b7b663badf7add3e2d1a583e0d1acb7583b3c57194b70`.
- Belum ada verifikasi Android 15, HP fisik, virtual autofill structure, serangan overlay, atau pembayaran end-to-end. Tidak ada request ke website client.

## v1.1.0 — desain ulang (versionCode 3, kandidat lokal)

- Build release/debug/instrumentation berhasil; 5 unit test lulus. Lint: 0 error, 27 warning (termasuk custom view yang dibuat programatis dan alokasi gambar ikon).
- Guard komponen gagal pada Spinner lama lalu lulus setelah penggantian; guard startup tetap lulus.
- Android 8/API 26: StartupTest, ComponentTest, RedesignTest, ResponsiveTest, StoreSecurityTest (2 tes), QueueBehaviorTest lulus — 7 tes runtime pada 6 kelas.
- ComponentTest awal menemukan class aksesibilitas kosong pada toggle custom; implementasi diperbaiki dan tes ulang lulus. Checked/checkable semantics serta FLAG_SECURE diverifikasi.
- Empat tab dan pilihan filter diuji pada debug instrumentation. Pemeriksaan UI Automator awal terhalang ANR System UI/Process system; tidak dinilai lulus. Setelah dialog sistem ditutup, pemeriksaan empat halaman debug berhasil. Label lama skrip “signed release” tidak dipakai sebagai bukti release: skrip kini menolak package DEBUGGABLE.
- APK signed v1.1.0 terpasang pada emulator API 26; dumpsys menunjukkan versionCode 3 dan tanpa DEBUGGABLE. Judul empat halaman serta scroll hingga Bantuan penyiapan terverifikasi. Screenshot final memperlihatkan aksi terakhir berada di atas navigasi, tidak tertutup.
- Pengujian resize layar sempit + font 1.6, landscape, dan tablet belum lulus: dump UiAutomator setelah resize tidak menghasilkan hierarki baru, kemudian emulator berhenti. Hasil XML lama tidak digunakan; helper kini menghapus dump lama dan mewajibkan pesan sukses dump. Belum ada bukti keyboard/formulir lengkap di berbagai ukuran atau HP fisik.
- File backend, worker, listener, penyimpanan, dan kontrak pembayaran tidak diubah. Tidak ada request ke website client.
- Android 15, HP fisik/OEM, dan pembayaran end-to-end tetap belum terverifikasi. ResponsiveTest memeriksa komponen teks besar/scroll, bukan sertifikasi semua ukuran layar.

SHA-256 APK v1.1.0:

```text
4a0c997daae63f6fdea9425d3dcc89de415f7ae7b3509af5028e5b7d41ca2c32
```

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
