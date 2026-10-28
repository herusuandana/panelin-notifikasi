# Panduan pengujian

[Unduh dan pasang](README.md) · [Hasil QA](QA-RESULTS.md) · [Privasi](PRIVACY.md)

Build/test bukan bukti keaslian pembayaran atau kompatibilitas semua HP. Catat **lulus**, **gagal**, atau **belum diuji**, versi aplikasi, Android, merek/model, package sumber, dan langkah reproduksi. Jangan bagikan notifikasi privat.

## Sebelum uji client

- Cocokkan checksum APK dan fingerprint sertifikat di [hasil QA](QA-RESULTS.md).
- Minta persetujuan pemilik perangkat, bukan PIN/password/OTP dompet.
- Utamakan staging. Untuk produksi, sepakati invoice, nominal, dan rekonsiliasi dengan client. **Jangan kirim payload pembayaran buatan ke callback produksi.**
- Preset backend yang diperiksa hanya DANA Bisnis. Cocokkan parser dengan notifikasi asli.

## Checklist perangkat

| Area | Kriteria |
| --- | --- |
| Instalasi bersih | Tanpa endpoint/token, transaksi contoh, atau izin diam-diam |
| Izin ditolak | UI tetap bisa dipakai, tidak mengaku siap memantau |
| Sumber | Hanya sumber terpilih dan aktif diproses, sumber lain tidak disimpan/dikirim |
| Konfigurasi | HTTP, query, fragment, dan userinfo ditolak, domain ditinjau sebelum simpan |
| Pembayaran nyata | Invoice dari website, nominal tepat, mutasi/riwayat/deposit/saldo cocok, hanya satu kredit/ledger |
| Duplikat/retry | Update notifikasi dan retry tidak menggandakan saldo, replay hanya di lingkungan terisolasi |
| Offline/pulih | Antrean bertahan lalu terkirim saat jaringan pulih, hapus riwayat tidak menghapus pending |
| Jeda | Pemantauan/sumber nonaktif menghentikan pengiriman berikutnya, permintaan berjalan mungkin selesai |
| Tujuan berubah | Endpoint/token baru menahan antrean lama, tidak mengalihkannya |
| Hasil server | 401/404/422 perlu perhatian, 5xx/429/jaringan dicoba ulang terbatas |
| Hasil deposit | ignored/unmatched/ambiguous/review/duplicate bukan kredit saldo |
| Filter | Promo, OTP, transaksi keluar/gagal bukan pembayaran masuk |
| Background | Uji layar mati, Doze, reboot, dan baterai, catat pengaturan OEM serta keterlambatan |
| Listener | Cabut/aktifkan izin, amati status dan pemulihan sambungan |
| Tampilan | Semua tab, daftar panjang/scroll, font besar, keyboard, rotasi, masking token |
| Pembaruan | Sertifikat sama dan versionCode lebih tinggi, konfigurasi/pending tetap tersimpan |
| Privasi | Tanpa token/payload di log, source, backup, atau screenshot yang dibagikan |

Force stop bisa memerlukan pembukaan ulang aplikasi. Pengaturan OEM tidak menjamin pengiriman real-time tanpa gangguan.

## Batas backend

Fingerprint backend yang diperiksa tidak memakai ID transaksi/waktu pembayaran tepercaya. Akun/package/judul/nominal identik bisa dianggap duplikat. Nominal dengan invoice historis bisa ditahan untuk review. Jangan longgarkan pengaman agar tes lulus. Periksa mutasi sebelum rekonsiliasi dan konfirmasi admin.

## Pemeriksaan pengembang

Dari root repository dengan JDK 17 dan Android SDK 35:

```sh
./gradlew testDebugUnitTest lintRelease assembleDebug assembleDebugAndroidTest
python3 scripts/check-release.py
python3 scripts/check-startup.py
python3 scripts/check-ui-components.py
python3 scripts/check-ui-copy.py
python3 scripts/check-wallet-design.py
```

`python3 scripts/build-release.py assembleDebug assembleDebugAndroidTest` menjalankan unit test, lint, build release, dan verifikasi signature. Ikuti [aturan signing](README.md), jangan ganti identitas resmi.

### Emulator terisolasi

Helper memakai `$HOME/.local/share/panelin-android-toolchain`: JDK 17, command-line tools, SDK 35/Build Tools 35.0.0, emulator, dan image Android 26 x86_64. `setup-local-sdk.py` memasang paket SDK setelah struktur toolchain tersedia, bukan installer JDK/Gradle lengkap. Sesuaikan lokasi dengan workstation.

```sh
python3 scripts/start-emulator.py
# Di terminal lain, setelah APK debug/instrumentation dibangun:
$HOME/.local/share/panelin-android-toolchain/android-sdk/platform-tools/adb -s emulator-5580 root
python3 scripts/device-check.py
# Untuk pemeriksaan navigasi APK release yang telah dipasang:
python3 scripts/check-release-ui.py
# Setelah APK debug dan instrumentation terpasang:
python3 scripts/check-responsive.py
```

**Jangan jalankan helper pada HP client berdata nyata.** Serial dipatok ke `emulator-5580`. device-check menghapus data/uninstall aplikasi fixture, lalu memasang APK release hanya jika seluruh tes lulus. Jika suite berhenti, langkah akhir tidak dijalankan.

Hasil per kelas dan screenshot ada di `dist/qa/`, tidak di-commit. Host tanpa KVM bisa mengalami timeout/ANR sistem. Periksa screenshot/hierarki, jangan nyatakan tes terhalang sebagai lulus.

| Pemeriksaan | Cakupan dan batas |
| --- | --- |
| `ComponentTest` | Checked state toggle Compose, checkbox nonaktif tidak menerima persetujuan |
| `RedesignTest` | Empat tab Compose, selector riwayat, tanpa tujuan bawaan |
| `ResponsiveTest` | Scroll ke bantuan, buka sheet, FLAG_SECURE, aksi penutup. Ulangi terpisah untuk font besar/ukuran lain, bukan bukti semua layout |
| `EditorSecurityTest` | Domain `.invalid`, penerusan dijeda, masking token, persetujuan wajib, keamanan Activity, pembatalan. Bukan bukti virtual autofill structure atau serangan overlay |
| `check-ui-components.py` | Guard source Compose, inset, proteksi root/window. Bukan bukti visual/runtime |
| `StartupTest` | Buka Activity dan periksa dashboard tanpa UI Automator/izin notifikasi. Ulangi di Android 15/API 35 untuk system bar modern |
| `check-startup.py` | Guard urutan source, bukan tes runtime |
| `StoreSecurityTest` | SQLite/Keystore nyata, penolakan URL tidak aman |
| `QueueBehaviorTest` | Pending, pause, hapus riwayat, credential-change hold. Bukan uji offline menyeluruh |
| `StatusNotificationTest` | Status tidak mengaku tersambung tanpa akses listener |
| `WorkerContractTest` | Worker/JSON/outcome dan jejak HTTP nyata, transport in-memory tanpa request eksternal |
| `TraceStoreTest` | Alasan skip tanpa isi, percobaan/retry, hapus riwayat tidak menghapus pending/dedup |
| `TraceMigrationTest` | Upgrade SQLite v1 ke v2 menjaga antrean/dedup, wajib proses dan database baru |
| `TraceRegressionTest` | Token lama disamarkan, antrean tidak tertutup promosi, recovery batas percobaan, dedup setelah hapus |
| `ResponseTraceTest` | HTTP 401/503, HTML, respons besar/kosong, timeout, dan penyaringan rahasia tanpa jaringan |
| `TraceUiTest` | Timeline, ID korelasi, label lokal/website, isi hanya dibuka saat diminta |
| `ClientSmokeTest` | Navigasi tanpa izin sensitif/data bawaan, hasil terakhir terbatas sesuai QA |

Pemeriksaan APK release memverifikasi judul halaman lewat hierarki aksesibilitas, bukan pembayaran end-to-end.
