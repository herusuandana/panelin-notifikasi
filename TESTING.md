# Panduan pengujian

[Pemasangan](README.md) · [Hasil QA](QA-RESULTS.md) · [Privasi](PRIVACY.md)

Catat lulus/gagal/belum diuji, versi aplikasi/Android, merek/model, package sumber, dan langkah reproduksi. Build/test tidak membuktikan keaslian pembayaran atau kompatibilitas semua HP. Jangan bagikan notifikasi privat.

## Sebelum uji client

- Cocokkan checksum dan sertifikat dengan [QA](QA-RESULTS.md). Minta persetujuan pemilik HP, bukan PIN/password/OTP.
- Utamakan staging. Di produksi, sepakati invoice, nominal, dan rekonsiliasi. Jangan kirim payload pembayaran buatan ke callback produksi.
- Preset backend yang diperiksa hanya DANA Bisnis. Cocokkan parser dengan notifikasi asli.

## Checklist perangkat

| Area | Yang diperiksa |
| --- | --- |
| Instalasi/izin | Tanpa tujuan/token, transaksi contoh, atau izin diam-diam. Izin ditolak tidak merusak UI atau mengaku siap |
| Sumber | Hanya sumber terpilih dan aktif diproses, sumber lain tidak disimpan/dikirim |
| Konfigurasi | HTTP, query, fragment, userinfo ditolak. Periksa domain sebelum simpan |
| Pembayaran | Invoice, nominal, mutasi, riwayat, deposit, dan saldo cocok. Hanya satu kredit/ledger |
| Duplikat/retry | Tidak menggandakan saldo. Uji replay hanya di lingkungan terisolasi |
| Offline/pulih | Antrean bertahan dan terkirim setelah jaringan pulih. Hapus riwayat tidak menghapus pending |
| Jeda | Pengiriman berikutnya berhenti. Permintaan berjalan mungkin selesai |
| Tujuan berubah | URL/token baru menahan antrean lama, bukan mengalihkannya |
| Respons | 401/404/422 perlu perhatian. 5xx/429/jaringan dicoba ulang terbatas |
| Kredit | ignored/unmatched/ambiguous/review/duplicate bukan kredit. HTTP 200 saja tidak cukup |
| Filter | Promo, OTP, transaksi keluar/gagal tidak dianggap pembayaran masuk |
| Background | Uji layar mati, Doze, reboot, baterai/OEM, dan keterlambatan |
| Listener | Cabut/aktifkan akses, periksa status dan pemulihan sambungan |
| UI | Empat tab, daftar panjang, font besar, keyboard, rotasi, mode gelap, dan masking token |
| Updater | Cek setiap buka/kembali ke depan. Popup hanya rilis lebih baru yang diizinkan. Stabil bawaan, rilis uji opsional |
| Persetujuan update | Unduh hanya atas pilihan pengguna, pemasangan dikonfirmasi Android. Konfigurasi/pending tetap ada |
| Validasi APK | Periksa SHA-256/package/versi/signature. Tolak APK rusak, debug, beda package/sertifikat, atau downgrade |
| Update gagal | Tanpa rilis baru atau jaringan/HTTP gagal, cek otomatis tetap diam |
| File update | APK privat, FileProvider hanya membagikan direktori pembaruan ke installer |
| Refresh/pilihan | Tarik tiga kali di tiap tab. Data lokal dibaca ulang, indikator kembali ke atas, skeleton beranimasi hanya saat memuat, tanpa pengiriman ulang webhook |
| Ikon | Riwayat memakai ikon package sumber. Jika package hilang, pakai fallback aplikasi, bukan jam |
| Privasi | Token/payload tidak masuk log, source, backup, atau screenshot yang dibagikan. Cek GitHub tidak membawa data pembayaran |

Force stop bisa memerlukan pembukaan ulang aplikasi. Pengaturan OEM tidak menjamin pengiriman tanpa gangguan. Jangan hapus data/uninstall saat masih ada antrean, rekonsiliasi lebih dulu.

## Batas backend

Fingerprint backend tidak memakai ID transaksi/waktu pembayaran tepercaya. Akun/package/judul/nominal sama bisa dianggap duplikat, nominal dengan invoice historis bisa ditahan untuk review. Jangan longgarkan pengaman agar tes lulus. Periksa mutasi sebelum rekonsiliasi/konfirmasi admin.

## Pemeriksaan pengembang

Dari root repo dengan JDK 17 dan Android SDK 35:

```sh
./gradlew testDebugUnitTest lintRelease assembleDebug assembleDebugAndroidTest
python3 scripts/check-release.py
python3 scripts/check-startup.py
python3 scripts/check-ui-components.py
python3 scripts/check-ui-copy.py
python3 scripts/check-compact-design.py
python3 scripts/check-refresh.py
python3 scripts/check-refresh-flow.py
python3 scripts/check-update-manifest.py
python3 scripts/check-updates.py
```

`python3 scripts/build-release.py assembleDebug assembleDebugAndroidTest` menjalankan unit test, lint, build release, dan verifikasi signature. Ikuti [aturan signing](README.md), jangan ganti identitas resmi.

### Emulator terisolasi

Helper memakai `$HOME/.local/share/panelin-android-toolchain`: JDK 17, command-line tools, SDK 35/Build Tools 35.0.0, emulator, dan image Android 26 x86_64. `setup-local-sdk.py` memasang paket SDK setelah struktur toolchain tersedia, bukan installer JDK/Gradle. Sesuaikan lokasi workstation.

```sh
python3 scripts/start-emulator.py
# Terminal lain, setelah APK debug/instrumentation dibangun:
$HOME/.local/share/panelin-android-toolchain/android-sdk/platform-tools/adb -s emulator-5580 root
python3 scripts/device-check.py
# Setelah APK release terpasang:
python3 scripts/check-release-ui.py
# Setelah APK debug/instrumentation terpasang:
python3 scripts/check-responsive.py
```

Jangan jalankan helper pada HP client berdata nyata. Serial dipatok `emulator-5580`. `device-check.py` menghapus data/uninstall fixture dan hanya memasang release jika semua tes lulus. Jika suite berhenti, langkah akhir tidak berjalan.

Log per kelas/screenshot ada di `dist/qa/`, tidak di-commit. Tanpa KVM, emulator bisa timeout/ANR sistem. Periksa hierarki/screenshot, jangan hitung tes terhalang sebagai lulus.

| Pemeriksaan | Cakupan dan batas |
| --- | --- |
| `CompactUiTest` | Ringkasan, tiga gesture refresh per tab dan offset indikator kembali nol, pilihan filter, ikon package tanpa launcher |
| `check-refresh-flow.py` | Coroutine refresh nyata dengan frame/pembaca fixture: 12 penyelesaian, daftar aplikasi, error, cancellation. Bukan gesture atau render Android |
| `UpdateSecurityTest` | Controller nyata dengan HTTPS fixture lokal: foreground, kanal, popup, pembatalan installer, penolakan arsip. Jalankan kelas sendiri karena handler URL bersifat process-global. Bukan bukti instalasi APK baru berhasil |
| `check-updates.py` | 10 tes kebijakan updater offline, bukan runtime Android |
| `ComponentTest` | Checked state Compose, checkbox nonaktif tidak menerima persetujuan |
| `RedesignTest` | Empat tab, selector riwayat, tanpa tujuan bawaan |
| `ResponsiveTest` | Scroll bantuan, sheet, FLAG_SECURE, aksi penutup. Ulangi per font/ukuran, bukan bukti semua layout |
| `EditorSecurityTest` | `.invalid`, penerusan dijeda, token tersembunyi, consent wajib, keamanan Activity, pembatalan. Bukan uji virtual autofill/overlay |
| `check-ui-components.py` | Guard Compose/inset/proteksi root/window, bukan visual/runtime |
| `StartupTest` | Buka dashboard tanpa UI Automator/izin notifikasi. Ulangi di API 35 untuk system bar modern |
| `check-startup.py` | Urutan source, bukan runtime |
| `StoreSecurityTest` | SQLite/Keystore nyata dan penolakan URL tidak aman |
| `QueueBehaviorTest` | Pending, jeda, hapus riwayat, perubahan kredensial. Bukan uji offline penuh |
| `StatusNotificationTest` | Tidak mengaku tersambung tanpa akses listener |
| `WorkerContractTest` | Worker/JSON/outcome/jejak HTTP, transport in-memory tanpa request eksternal |
| `TraceStoreTest` | Alasan skip tanpa isi, percobaan/retry, hapus tidak membuang pending/dedup |
| `TraceMigrationTest` | SQLite v1 → v2 menjaga antrean/dedup, wajib proses/database baru |
| `TraceRegressionTest` | Token lama disamarkan, promo tidak menutup antrean, recovery batas percobaan, dedup setelah hapus |
| `ResponseTraceTest` | 401/503, HTML, respons besar/kosong, timeout, penyaringan rahasia tanpa jaringan |
| `TraceUiTest` | Timeline, ID korelasi, label lokal/website, isi hanya saat diminta |
| `ClientSmokeTest` | Navigasi tanpa izin sensitif/data bawaan, batas hasil di QA |

Cek APK release memverifikasi judul halaman lewat hierarki aksesibilitas, bukan pembayaran end-to-end.
