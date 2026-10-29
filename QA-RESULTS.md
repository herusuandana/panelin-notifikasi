# Hasil pengujian

## v1.7.0 — rilis normal (versionCode 10)

- APK menyerahkan format pembayaran ke website. Perlindungan pesan sensitif, sumber terpilih, antrean terenkripsi, dan deduplikasi tetap. Tujuan lama menunggu persetujuan ulang, bukan dihapus.
- Tujuh tes kebijakan forwarding/hasil dan 10 tes kebijakan updater lulus offline. Pemeriksaan coroutine refresh dan guard source lulus. Seluruh 18 unit test Gradle lulus tanpa gagal/skip.
- Build pertama terhenti SIGTERM sebelum verifikasi release. APK debug dan APK instrumentation sudah dikompilasi, bukan bukti runtime. Build kedua berhasil: release/debug/APK instrumentation selesai, source snapshot cocok, lint 0 error/25 warning. APK non-debuggable dengan signature v2 dan sertifikat resmi. SHA-256: `84f10431bd86fd2649d4a98e182ac0291797ec478c24e683a380ab1df21d5f3f`.
- Endpoint `/check` website pusat lulus 17 tes fitur pembayaran (263 assertion), termasuk autentikasi dan tidak ada perubahan saldo/deposit/log meski payload cocok. Tidak ada pengiriman uji ke client produksi.
- Fixture Android ditambahkan untuk request koneksi dan persetujuan ulang. Belum dijalankan: tidak ada perangkat ADB dan emulator software sebelumnya terhalang ANR sistem. Build/kompilasi bukan bukti runtime, TLS nyata, atau pembayaran hingga kredit.
- Pengguna melaporkan refresh dan updater v1.6.1 normal pada HP-nya. Ini bukan pengujian fitur v1.7.0 atau semua perangkat.
- Suite backend pertama: 1559 lulus, satu gagal karena direktori temp Chrome tidak dapat diakses user `www`. Direktori writable yang terlalu panjang juga gagal membuat socket browser. Dengan `TMPDIR` pendek/writable, 10 tes invoice lulus tanpa perubahan kode PDF. Suite penuh kedua: 1559 lulus, satu gagal pada tes rata-rata katalog (5401 vs 5400 detik). Tes memakai beberapa `now()` tanpa waktu beku dan lulus saat dijalankan sendiri (3 assertion). Tidak ada perubahan kode/assertion katalog. Suite penuh belum seluruhnya hijau.

## v1.6.1 — rilis normal (versionCode 9)

- Memperbaiki balapan status refresh cepat: pekerjaan dimulai dari `LaunchedEffect` setelah komposisi memuat diterapkan, lalu menunggu frame sebelum membaca data. Tidak ada penundaan visual buatan.
- Skeleton berdenyut hanya saat pemuatan awal atau refresh manual. Refresh membaca data lokal/status izin, bukan saldo website dan bukan replay webhook.
- Guard source refresh gagal sebelum patch, lalu lulus. Pemeriksaan coroutine nyata dengan frame/pembaca fixture lulus: 12 penyelesaian, daftar aplikasi, error, dan cancellation. Ini bukan bukti gesture/render Android.
- Tes gesture diperkuat menjadi tiga tarikan di tiap tab, memeriksa kenaikan generasi tepat satu dan offset indikator kembali nol. Tes perangkat belum dijalankan: ADB tidak memiliki perangkat, host tanpa KVM, percobaan emulator sebelumnya gagal menyiapkan layanan Android.
- Release/debug/APK tes berhasil dibangun. Unit test: 17 lulus, tanpa gagal atau skip. Lint release: 0 error, 25 warning. Source sebelum/sesudah build cocok.
- APK release non-debuggable, signature v2 valid dengan sertifikat resmi yang sama. SHA-256: `3acee595bc90017abf0b2a08b10b103a15fea6e1df9de873298486e288742e32`.
- Dipublikasikan sebagai rilis normal sesuai permintaan pemilik. Label tersebut tidak menghapus batas QA: gesture/skeleton di HP, UI/editor, API 35, matrix responsif, installer nyata, dan pembayaran hingga kredit belum terverifikasi. Screenshot tetap berlabel v1.6.0. Tidak ada request uji ke webhook client, filter pembayaran tidak berubah.

## v1.6.0 — prerelease uji terbatas (versionCode 8)

- Release/debug/APK tes berhasil dibangun. 17 unit test lulus. Lint: 0 error, 25 warning. QA runtime belum lengkap.
- Signature v2 valid dengan sertifikat resmi yang sama. SHA-256 APK: `fbc8338f9660e5c7bc6aa91ea174b9c1e131555466b4c49850b07c59fb2e53db`.
- Source sebelum/sesudah build cocok. Pemeriksaan UI compact, copy, dan 10 tes kebijakan updater lulus.
- Guard manifest updater lulus untuk provider privat khusus pembaruan dan installer dengan konfirmasi Android. Ini pemeriksaan source, bukan bukti runtime.
- Navigasi empat tab APK signed non-debuggable v1.6.0/code 8 lulus di API 26. Screenshot Pengaturan terbaca tanpa overlap.
- Runtime awal API 26: TraceUiTest (3) dan ComponentTest (2) lulus. UpdateSecurityTest gagal launch Activity, CompactUiTest timeout/error pengukuran Compose, EditorSecurityTest timeout idle, RedesignTest error pengukuran. Rerun terisolasi disimpan terpisah.
- Rerun terisolasi: UpdateSecurityTest lulus (1 tes), termasuk foreground berulang, hasil kosong/error yang diam, kanal stabil/uji, popup, pembatalan installer, dan arsip tidak aman. Ini bukan bukti sukses memasang APK baru. Rerun CompactUiTest terputus (ADB exit 255), editor/Redesign tidak sempat diulang. Kernel mengonfirmasi qemu PID 1237809 dibunuh batas memori cgroup (anon RSS 4168300 kB). Pemulihan pengaturan akhir tidak terverifikasi.
- Belum terbukti: pemasangan APK baru/izin installer nyata, matrix responsif lengkap, mode gelap, API 35, HP fisik, dan pembayaran. Tidak ada pengiriman uji ke webhook client.

## v1.5.0 — prerelease uji terbatas (versionCode 7)

- Release/debug/APK tes berhasil dibangun. Tujuh unit test lulus. Lint: 0 error, 27 warning.
- Signature v2 valid, sertifikat sama. SHA-256 APK: `9440f925865b3c51589bbb18dd4b6e51a241fe9ba914763647bcc2b79000e41a`.
- Guard desain, copy, komponen, versi, dan diff lulus, bukan bukti visual/runtime.
- Navigasi empat tab APK signed non-debuggable lulus di API 26. Beranda terbaca tanpa overlap pada ukuran dasar.
- API 26: 12 tes runtime lulus. Navigasi/pintasan/scroll lulus di HP, compact/font 1.6, dan landscape. Komponen/timeline lulus di HP, editor/consent di compact/font 1.6.
- Matriks belum lulus seluruhnya: editor HP gagal `ComposeNotIdleException`, editor landscape terputus exit 255. Kernel mencatat qemu dibunuh batas memori cgroup. Penyebab timeout editor belum diketahui. Tablet belum diuji, cleanup gagal.
- Mode gelap runtime, Android 15, HP fisik, dan pembayaran nyata belum terverifikasi. Tidak ada pengiriman uji ke webhook client.

## v1.4.0 — prerelease uji terbatas (versionCode 6)

- Tujuh unit test lulus. Lint: 0 error, 26 warning. Release/debug/instrumentation berhasil dibangun setelah review. Signature v2 valid, sertifikat sama.
- API 26: 17 tes lulus pada 13 kelas. StartupTest dan ClientSmokeTest awalnya gagal, lalu lulus saat diulang tanpa perubahan aplikasi. Bukan satu suite yang seluruhnya lulus.
- Matriks: phone 4/5, editor tidak menemukan node token. Emulator dibunuh batas memori pada ukuran berikutnya. Cleanup gagal, tiga ukuran lain belum diuji.
- Navigasi signed belum terverifikasi. ANR System UI dan instalasi timeout, termasuk non-streaming yang macet lalu dihentikan bersama emulator. Bukan bukti crash aplikasi.
- Runner kemudian mengisolasi state per kelas dan menyimpan error. Sintaks lulus, matriks belum diulang setelah perubahan.
- Runtime pertama terputus di TraceStoreTest: harness Wi-Fi shell tidak berizin dan ADB offline. Tidak dihitung lulus. Harness kini memakai root emulator untuk jaringan.
- SHA-256 APK: `d142dbf4d4a4a0f73e6f132471caa314d3969e7f87fe9c5ad5740e17df264168`.
- HP fisik/pembayaran nyata belum diuji. Tidak ada request ke webhook client. Publikasi prerelease bukan bukti seluruh QA lulus.

## v1.3.0 — build lokal (versionCode 5)

- Release/debug/instrumentation berhasil dibangun. Lima unit test lulus. Lint: 0 error, 26 warning.
- Signature v2 valid, sertifikat sama. Package `id.panelin.notifikasi`, minSdk 26, target 35, non-debuggable.
- SHA-256 APK: `cc0ede1cd2e3b98d242d33507d93cd361cf17fbeb3419e662b47b2b45df4eee3`.
- Guard startup, komponen, copy, versi, diff, dan tautan lokal lulus.
- Empat tab APK signed final lulus lewat hierarki aksesibilitas. Beranda terbaca tanpa overlap pada ukuran dasar. Screenshot: `dist/qa/release-*.png`.
- Build sebelumnya lulus 12 tes API 26. Suite final lulus Startup (1), Component (2), Redesign (1), lalu terputus di ResponsiveTest. Kernel mencatat OOM qemu akibat batas memori cgroup, bukan crash aplikasi. Pengulangan memakai emulator lebih ringan.
- Matriks final API 26 lulus 8 tes: masing-masing 2 di HP 720×1280, sempit 640×1136/font 1.6, landscape 1280×720, dan tablet 1000×800. Cakupan: navigasi, scroll, editor/token, persetujuan, dan Simpan dengan keyboard terbuka.
- Percobaan pertama gagal pada selector LazyColumn dan ANR sistem. Tes kemudian menggulir ke kolom, termasuk setelah rekreasi Activity. Aplikasi tidak berubah saat diulang.
- Setelah tes lulus, emulator berhenti saat pemulihan density. Harness exit 1 pada cleanup. Pemulihan density/font dan pemasangan ulang release belum selesai. Log: `dist/qa/responsive/*.txt`.
- ANR System UI/Process system dicatat terpisah. Percobaan gagal/terputus tidak dihitung lulus.
- Belum dipublikasikan. HP fisik/pembayaran nyata belum diuji, tanpa request ke website client.

## v1.2.0 — Jetpack Compose (versionCode 4, uji terbatas)

- Release/debug/instrumentation berhasil dibangun, sertifikat sama. Lima unit test lulus. Lint: 0 error, 23 warning.
- API 26 awal: StartupTest (1), ComponentTest (2), RedesignTest (1) lulus. ResponsiveTest gagal mencari item LazyColumn yang belum terkomposisi, lalu lulus setelah memakai scroll-to-node.
- ANR System UI/Process system menghalangi tes, tidak dihitung lulus. Konfigurasi 640×1136/font 1.6 direset ke 720×1280/font 1.0 sebelum suite.
- APK final setelah penyesuaian navigasi teks besar berhasil dibangun. Empat halaman signed non-debuggable lulus lewat hierarki aksesibilitas. Preview saat itu berasal dari APK, bukan mockup.
- Ukuran APK 1.31 MiB, sebelumnya 1.91 MiB. R8/resource shrinking aktif, bukan benchmark RAM/CPU.
- Runtime API 26 lulus 11 tes pada 9 kelas: Startup 1, Component 2, Redesign 1, Responsive 1, EditorSecurity 1, StoreSecurity 2, QueueBehavior 1, StatusNotification 1, WorkerContract 1.
- StatusNotificationTest awal gagal setelah cancel, lalu lulus dalam 2.764 detik setelah menunggu acknowledgement NotificationManager maksimal 5 detik. Penerusan tidak berubah.
- ClientSmokeTest tetap gagal saat diulang: `Error while connecting UiAutomation`. Hasil akhir 11 lulus, 1 gagal/terhalang. Suite keseluruhan tidak lulus. Navigasi signed lulus terpisah.
- Uji gabungan sebelumnya gagal pada rekreasi Activity dan status. Setelah memakai ActivityScenario, editor/token/consent lulus. Hasil v1.1.0 tidak otomatis berlaku untuk Compose.
- SHA-256 APK: `2fa6c9783cca3e28ef0b7b663badf7add3e2d1a583e0d1acb7583b3c57194b70`.
- Android 15, HP fisik, virtual autofill structure, serangan overlay, dan pembayaran end-to-end belum diverifikasi. Tidak ada request ke website client.

## v1.1.0 — desain ulang (versionCode 3, kandidat lokal)

- Release/debug/instrumentation berhasil dibangun. Lima unit test lulus. Lint: 0 error, 27 warning, termasuk custom view programatis dan alokasi ikon.
- Guard komponen gagal pada Spinner lama lalu lulus setelah diganti. Guard startup lulus.
- API 26: 7 tes pada 6 kelas lulus, yaitu StartupTest, ComponentTest, RedesignTest, ResponsiveTest, StoreSecurityTest (2), dan QueueBehaviorTest.
- ComponentTest menemukan class aksesibilitas toggle kosong, lalu lulus setelah perbaikan. Checked/checkable semantics dan FLAG_SECURE terverifikasi.
- Empat tab/filter diuji lewat debug instrumentation. UI Automator awal terhalang ANR sistem, lalu empat halaman debug lulus setelah dialog ditutup. Label skrip lama "signed release" bukan bukti release. Skrip kini menolak DEBUGGABLE.
- Signed terpasang di API 26, dumpsys menunjukkan code 3 tanpa DEBUGGABLE. Judul empat halaman dan scroll ke Bantuan penyiapan terverifikasi, aksi terakhir di atas navigasi.
- Resize sempit/font 1.6, landscape, dan tablet belum lulus: dump tidak menghasilkan hierarki baru lalu emulator berhenti. XML lama tidak dipakai. Helper kini menghapus dump lama dan mewajibkan pesan sukses. Keyboard/form lintas ukuran belum terbukti.
- Backend, worker, listener, penyimpanan, dan pembayaran tidak berubah. Tanpa request ke website client.
- Android 15, HP fisik/OEM, dan pembayaran end-to-end belum terverifikasi. ResponsiveTest hanya mencakup teks besar/scroll, bukan semua ukuran.
- SHA-256 APK: `4a0c997daae63f6fdea9425d3dcc89de415f7ae7b3509af5028e5b7d41ca2c32`.

## v1.0.1 — perbaikan startup (versionCode 2)

- `window.insetsController` dipindah setelah `setContentView(root)`. Getter PhoneWindow mengakses decor langsung, safe-call Kotlin tidak mencegah exception jika decor belum tersedia.
- `python3 scripts/check-startup.py` gagal pada urutan lama lalu lulus setelah perbaikan. Guard source, bukan runtime.
- StartupTest ditambahkan untuk membuka Activity dan memeriksa dashboard. Release/debug/instrumentation berhasil dibangun, lima unit test lulus, lint tanpa error.
- Sertifikat sama dengan v1.0.0, code naik 1 → 2 untuk pembaruan tanpa uninstall.
- Android 15 belum terverifikasi: emulator API 35 software tanpa KVM belum selesai boot. Log HP pelapor belum diterima. Hubungan crash dengan defect startup belum dikonfirmasi stack trace.
- Izin/webhook tidak berubah, pembayaran produksi tidak diuji.
- SHA-256 APK: `7851eb8f552288b0906fa2bdf925b8e02bbdbdcb81059bb5c2bad762de8395c4`.

## v1.0.0 — arsip

Diuji 27 Oktober 2026. Package `id.panelin.notifikasi`, versionCode `1`, minimum Android 8.0/API 26, target SDK 35. Signed untuk uji terbatas, bukan produksi. Bukti hanya dari build lokal/emulator, tanpa pembayaran nyata atau request webhook produksi.

### Hasil

| Pemeriksaan | Hasil |
| --- | --- |
| Unit test | 5 tes, 0 failure/error |
| Lint release | 0 error, 21 warning |
| Release/debug/instrumentation | Berhasil dibangun |
| Signature | Valid v2, bukan debug key |
| Instalasi release | Berhasil, package/versi sesuai, non-debuggable |
| StoreSecurityTest | 2 lulus di Android 8 |
| QueueBehaviorTest | 1 lulus: jeda, hapus riwayat, perubahan tujuan |
| StatusNotificationTest | 1 lulus tanpa akses listener |
| WorkerContractTest | 1 lulus dengan transport in-memory |
| Navigasi release | Judul empat tab terverifikasi |
| Versi/changelog | Scroll Pengaturan, versi 1.0.0 (1), dialog terbuka |
| Arsip source | ZIP valid tanpa keystore, kredensial lokal, local.properties |

SQLite/Keystore Android nyata digunakan. Token/endpoint tidak ditemukan plaintext di database/preferences yang diperiksa, bukan audit forensik penuh. Worker/JSON nyata dengan transport in-memory tidak membuktikan TLS atau integrasi Laravel end-to-end.

Screenshot saat itu terbaca, tetapi gambar di [README](README.md) kini berasal dari v1.5.0. Lint mencatat dependensi, alokasi ikon, resource launcher, string Indonesia, dan commit preferensi. Tidak ada baseline penyembunyi error.

### Gagal dan belum diuji

- Android 8 menolak PRAGMA `secure_delete=ON` lewat `execSQL` karena menghasilkan baris. Setelah memakai `rawQuery` dan memeriksa hasil, dua tes penyimpanan yang gagal lulus di emulator sama.
- ClientSmokeTest tidak lulus, tertutup dialog "Process system isn't responding". Emulator tanpa KVM juga mengalami timeout/ANR System UI.
- Pemeriksaan release terpisah berhasil membuka empat tab/changelog tanpa dialog sistem. Hanya bukti navigasi dasar, bukan seluruh suite/perangkat.
- Belum diuji: pembayaran → webhook Laravel → kredit, listener/offline/reboot/Doze/OEM pada HP fisik, seluruh font/keyboard/rotasi/Android/merek, upgrade tanpa kehilangan konfigurasi/antrean, serta format DANA pribadi/sumber lain.

### Identitas APK

APK: `panelin-notifikasi-1.0.0-release.apk`

SHA-256 APK: `52be0707147dc58225f399b459eb623be862251dfff32573c44e46666139db8d`

Sertifikat: `CN=Panelin Android Release, O=Panelin, C=ID`

SHA-256 sertifikat: `70f30880391bd102c4392f1ef489fc58bd6f90d15a405a1ecbd068b22fe634e8`

Cocokkan `.sha256` dan fingerprint saat menerima APK. Pembaruan resmi wajib memakai sertifikat sama. [Panduan uji](TESTING.md).
