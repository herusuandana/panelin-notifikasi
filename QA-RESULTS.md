# Hasil pengujian

## v1.5.0 — prerelease uji terbatas (versionCode 7)

- Release, debug, dan APK tes berhasil dibangun. Tujuh unit test lulus. Lint: 0 error, 27 warning.
- Signature v2 valid, sertifikat pembaruan tetap sama. SHA-256 APK: `9440f925865b3c51589bbb18dd4b6e51a241fe9ba914763647bcc2b79000e41a`.
- Guard desain, copy, kontrak komponen, metadata versi, dan diff lulus. Bukan bukti visual/runtime.
- Navigasi empat tab APK signed v1.5.0/code 7 non-debuggable lulus di API 26. Screenshot Beranda terbaca tanpa overlap pada ukuran dasar.
- API 26: 12 tes runtime lulus. Navigasi/pintasan dan scroll lulus di HP, compact/font 1.6, serta landscape. Komponen dan timeline lulus pada HP. Editor/consent lulus pada compact/font 1.6.
- Matriks belum seluruhnya lulus: editor HP gagal ComposeNotIdleException, editor landscape terputus dengan exit 255. Kernel mengonfirmasi qemu dibunuh batas memori cgroup. Tablet belum diuji dan cleanup gagal. Penyebab timeout editor belum dipastikan.
- Tablet, mode gelap runtime, Android 15, HP fisik, dan pembayaran nyata belum terverifikasi. Hasil versi sebelumnya bukan bukti kelulusan versi ini.
- Tidak ada pengiriman uji ke webhook klien.

## v1.4.0 — prerelease uji terbatas (versionCode 6)

- Tujuh unit test lulus. Lint: 0 error, 26 warning.
- Release, debug, dan instrumentation berhasil dibangun setelah review. Signature v2 valid dengan sertifikat yang sama.
- API 26: 17 tes lulus pada 13 kelas. StartupTest dan ClientSmokeTest gagal pada percobaan awal, lalu masing-masing lulus saat diulang tanpa perubahan aplikasi. Bukan satu suite yang seluruhnya hijau.
- Matriks belum lulus: phone 4/5, editor tidak menemukan node token. Emulator dibunuh batas memori saat ukuran berikutnya. Cleanup gagal, tiga ukuran lain belum diuji.
- Navigasi signed belum terverifikasi. Emulator baru mengalami ANR System UI dan instalasi timeout. Alternatif instalasi non-streaming juga macet, lalu dihentikan bersama emulator. Bukan bukti crash aplikasi.
- Runner matriks kini mengisolasi state per kelas dan menyimpan keluaran error. Pemeriksaan sintaks lulus, matriks setelah perubahan runner belum dijalankan.
- Percobaan runtime pertama terputus pada TraceStoreTest. Harness Wi-Fi shell tidak berizin dan ADB offline, bukan kelulusan. Jaringan kini diatur harness root emulator.
- SHA-256 APK: `d142dbf4d4a4a0f73e6f132471caa314d3969e7f87fe9c5ad5740e17df264168`.
- Belum ada verifikasi HP fisik atau pembayaran nyata. Tidak ada request ke webhook klien selama pengujian. Publikasi prerelease tidak berarti seluruh QA lulus.

## v1.3.0 — build lokal (versionCode 5)

- Release, debug, dan instrumentation berhasil dibangun. Lima unit test lulus. Lint: 0 error, 26 warning.
- Signature v2 valid, sertifikat release tetap sama. Package `id.panelin.notifikasi`, minSdk 26, target 35, tidak debuggable.
- SHA-256 APK: `cc0ede1cd2e3b98d242d33507d93cd361cf17fbeb3419e662b47b2b45df4eee3`.
- Guard startup, komponen, teks UI, metadata rilis, dan diff lulus. Tautan dokumen lokal valid.
- Navigasi empat tab pada APK signed final lulus melalui hierarki aksesibilitas. Screenshot artefak ini tersimpan di `dist/qa/release-*.png`. Tampilan Beranda terbaca tanpa overlap pada ukuran dasar.
- Build pemeriksaan sebelumnya lulus 12 tes runtime API 26. Suite final lulus Startup (1), Component (2), dan Redesign (1), lalu terputus saat ResponsiveTest karena emulator dihentikan batas memori cgroup. Log kernel mengonfirmasi OOM pada proses qemu, bukan crash aplikasi. Matriks diulang dengan emulator lebih ringan.
- Matriks final API 26: 8 tes lulus, masing-masing 2 pada HP 720×1280, layar sempit 640×1136/font 1.6, landscape 1280×720, dan tablet 1000×800. Mencakup navigasi, scroll, editor/token, serta akses persetujuan dan Simpan dengan keyboard terbuka.
- Percobaan pertama gagal pada selector LazyColumn dan ANR sistem. Tes kini menggulir ke kolom sebelum mengisinya, termasuk setelah rekreasi Activity. Kode aplikasi tidak berubah saat pengulangan.
- Setelah seluruh tes lulus, emulator berhenti saat pemulihan density. Harness exit 1 pada cleanup, bukan tes. Pemulihan density/font dan pemasangan ulang release belum selesai. Log: `dist/qa/responsive/*.txt`.
- ANR System UI/Process system dicatat terpisah. Percobaan gagal atau terputus tidak dihitung lulus.
- Belum diuji pada HP fisik atau pembayaran nyata. Tidak ada request ke website client. Build belum dipublikasikan.

## Arsip versi sebelumnya

## v1.2.0 — Jetpack Compose (versionCode 4, uji terbatas)

- Build signed/debug/instrumentation migrasi berhasil dengan sertifikat sama. Lima unit test lulus. Lint: 0 error, 23 warning.
- Runtime API 26 awal: StartupTest (1), ComponentTest (2), RedesignTest (1) lulus. ResponsiveTest gagal mencari item LazyColumn yang belum terkomposisi, lalu lulus setelah tes memakai scroll-to-node.
- ANR System UI/Process system sempat menghalangi tes, tidak dihitung lulus. Sisa konfigurasi 640×1136/font 1.6 direset ke 720×1280/font 1.0 sebelum suite.
- APK final setelah penyesuaian navigasi teks besar berhasil dibangun. Empat halaman APK signed non-debuggable lulus pemeriksaan hierarki aksesibilitas. Preview berasal dari APK ini, bukan mockup.
- APK 1.2.0: 1.31 MiB, sebelumnya 1.91 MiB. R8 dan resource shrinking aktif. Ukuran bukan benchmark RAM/CPU.
- Runtime API 26: Startup 1, Component 2, Redesign 1, Responsive 1, EditorSecurity 1, StoreSecurity 2, QueueBehavior 1, StatusNotification 1, WorkerContract 1 lulus. Total 11 tes pada 9 kelas.
- StatusNotificationTest awal gagal setelah cancel. Tes ulang lulus dalam 2.764 detik setelah menunggu acknowledgement NotificationManager maksimal 5 detik, tanpa perubahan penerusan. ClientSmokeTest tetap gagal setelah pengulangan: `Error while connecting UiAutomation`. Hasil terakhir: 11 lulus, 1 gagal/terhalang. **Suite keseluruhan tidak dinyatakan lulus.** Navigasi APK signed lulus secara terpisah.
- Uji gabungan sebelumnya gagal pada rekreasi Activity dan status, tetap dicatat gagal. Rekreasi kemudian memakai ActivityScenario, editor/token/consent lulus. Hasil v1.1.0 tidak otomatis berlaku untuk Compose.
- SHA-256 APK 1.2.0: `2fa6c9783cca3e28ef0b7b663badf7add3e2d1a583e0d1acb7583b3c57194b70`.
- Android 15, HP fisik, virtual autofill structure, serangan overlay, dan pembayaran end-to-end belum diverifikasi. Tidak ada request ke website client.

## v1.1.0 — desain ulang (versionCode 3, kandidat lokal)

- Build release/debug/instrumentation berhasil. Lima unit test lulus. Lint: 0 error, 27 warning, termasuk custom view programatis dan alokasi gambar ikon.
- Guard komponen gagal pada Spinner lama lalu lulus setelah diganti. Guard startup lulus.
- Android 8/API 26: StartupTest, ComponentTest, RedesignTest, ResponsiveTest, StoreSecurityTest (2), QueueBehaviorTest lulus. Total 7 tes pada 6 kelas.
- ComponentTest menemukan class aksesibilitas toggle kosong, lalu lulus setelah diperbaiki. Checked/checkable semantics dan FLAG_SECURE terverifikasi.
- Empat tab dan filter diuji lewat debug instrumentation. UI Automator awal terhalang ANR System UI/Process system, tidak dihitung lulus. Empat halaman debug lulus setelah dialog sistem ditutup. Label lama skrip “signed release” bukan bukti release. Skrip kini menolak package DEBUGGABLE.
- APK signed terpasang di API 26. dumpsys menunjukkan versionCode 3 tanpa DEBUGGABLE. Judul empat halaman dan scroll hingga Bantuan penyiapan terverifikasi. Screenshot menunjukkan aksi terakhir di atas navigasi.
- Resize layar sempit/font 1.6, landscape, dan tablet belum lulus. Dump tidak menghasilkan hierarki baru, lalu emulator berhenti. XML lama tidak dipakai. Helper kini menghapus dump lama dan mewajibkan pesan sukses. Keyboard/formulir lintas ukuran dan HP fisik belum terbukti.
- Backend, worker, listener, penyimpanan, dan kontrak pembayaran tidak berubah. Tidak ada request ke website client.
- Android 15, HP fisik/OEM, dan pembayaran end-to-end belum terverifikasi. ResponsiveTest mencakup teks besar/scroll, bukan semua ukuran layar.

SHA-256 APK v1.1.0:

```text
4a0c997daae63f6fdea9425d3dcc89de415f7ae7b3509af5028e5b7d41ca2c32
```

## v1.0.1 — perbaikan startup (versionCode 2)

- Akses `window.insetsController` dipindah setelah `setContentView(root)`. Getter Android PhoneWindow mengakses decor langsung. Safe-call Kotlin tidak mencegah exception jika decor belum tersedia.
- `python3 scripts/check-startup.py` gagal pada urutan lama lalu lulus setelah perbaikan. Ini guard source, bukan runtime.
- Instrumentation `StartupTest` ditambahkan untuk membuka Activity dan memeriksa dashboard. APK tes berhasil dikompilasi.
- Build release/debug/instrumentation berhasil. Lima unit test lulus, lint release tanpa error.
- Sertifikat sama dengan v1.0.0. versionCode naik dari 1 ke 2 untuk pembaruan tanpa uninstall.
- **Runtime Android 15 belum terverifikasi.** Emulator API 35 software tanpa KVM belum selesai boot. Log HP pelapor belum diterima, sehingga kaitan crash dengan defect startup belum dikonfirmasi lewat stack trace.
- Izin dan kontrak webhook tidak berubah. Pembayaran produksi tidak diuji.

SHA-256 APK v1.0.1:

```text
7851eb8f552288b0906fa2bdf925b8e02bbdbdcb81059bb5c2bad762de8395c4
```

## v1.0.0 — arsip

Diuji 27 Oktober 2026. Package `id.panelin.notifikasi`, versionCode `1`, minimum Android 8.0/API 26, target SDK 35.

**APK signed untuk uji terbatas, bukan siap produksi.** Hasil berasal dari build lokal dan emulator. Tidak ada pembayaran nyata atau webhook pembayaran ke website client produksi.

### Hasil terverifikasi

| Pemeriksaan | Hasil |
| --- | --- |
| Gradle unit test | 5 tes, 0 failure/error |
| Lint release | 0 error, 21 warning |
| Build release/debug/instrumentation | Berhasil |
| Signature release | Valid, APK Signature Scheme v2, bukan debug key |
| Instalasi release | Berhasil, package/versi sesuai, tidak debuggable |
| Penyimpanan Android 8 | 2 StoreSecurityTest lulus |
| Jeda, hapus riwayat, perubahan tujuan | 1 QueueBehaviorTest lulus |
| Status tanpa akses listener | 1 StatusNotificationTest lulus |
| Worker JSON/outcome, transport in-memory | 1 WorkerContractTest lulus |
| Navigasi APK release | Judul konten keempat tab terverifikasi |
| Versi/changelog | Scroll Pengaturan, versi 1.0.0 (1), dialog changelog berhasil |
| Arsip source | ZIP valid, tanpa keystore, kredensial lokal, local.properties |

Tes memakai SQLite/Keystore Android nyata. Token/endpoint tidak ditemukan plaintext di database/preferences yang diperiksa, bukan audit forensik menyeluruh. WorkerContractTest memakai worker/JSON nyata dengan transport in-memory, bukan bukti TLS atau integrasi Laravel end-to-end.

Screenshot memperlihatkan logo, teks, navigasi, dan changelog terbaca. Contoh: [Beranda](docs/images/beranda.png), [Riwayat](docs/images/riwayat.png). Lihat [panduan uji](TESTING.md).

Warning lint mencakup versi dependensi, alokasi gambar ikon, resource launcher, string Indonesia, dan commit preferensi. Tidak ada baseline penyembunyi error.

### Temuan dan batas emulator

- Android 8 menolak PRAGMA `secure_delete=ON` lewat `execSQL` karena menghasilkan baris. Setelah memakai `rawQuery` dan memeriksa hasil, dua tes penyimpanan yang gagal lulus di emulator sama.
- ClientSmokeTest tidak lulus karena dialog “Process system isn't responding” menutupi UI. Emulator software tanpa KVM juga mengalami timeout/ANR System UI. Hasil gagal tetap dicatat.
- Pemeriksaan APK release terpisah berhasil membuka empat tab dan changelog tanpa dialog sistem. Ini hanya bukti navigasi dasar artefak release, bukan kelulusan suite atau semua perangkat.

### Belum diverifikasi

- Notifikasi pembayaran nyata → webhook Laravel → kredit saldo.
- Listener, offline/pulih, reboot, Doze, dan pembatasan vendor pada HP fisik.
- Font besar, keyboard, rotasi, Android terbaru, dan berbagai merek HP secara menyeluruh.
- Pembaruan dengan versionCode lebih tinggi tanpa kehilangan konfigurasi/antrean.
- Semua format DANA pribadi atau sumber lain.

### Identitas APK yang diuji

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

Cocokkan `.sha256` dan fingerprint saat menerima APK. Pembaruan resmi harus memakai sertifikat sama.
