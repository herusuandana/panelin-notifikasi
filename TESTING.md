# Panduan pengujian

[Unduh dan pasang](README.md) · [Hasil v1.0.0](QA-RESULTS.md) · [Privasi](PRIVACY.md)

Build/test tidak membuktikan keaslian pembayaran atau kompatibilitas semua HP. Catat hasil sebagai **lulus**, **gagal**, atau **belum diuji**, beserta versi aplikasi, Android, merek/model, package sumber, dan langkah reproduksi. Jangan menyebarkan isi notifikasi privat.

## Sebelum uji client

- Cocokkan checksum APK dan fingerprint sertifikat release di [hasil QA](QA-RESULTS.md).
- Dapatkan persetujuan pemilik perangkat; jangan meminta PIN/password/OTP dompet.
- Utamakan website staging. Jika memakai produksi, sepakati invoice/nominal dan rekonsiliasi nyata dengan client. **Jangan mengirim payload pembayaran buatan ke callback produksi.**
- Preset backend yang diperiksa adalah DANA Bisnis, bukan seluruh format DANA. Pastikan parser sesuai notifikasi asli.

## Checklist penerimaan perangkat

| Area | Kriteria |
| --- | --- |
| Instalasi bersih | Tidak ada endpoint/token, transaksi contoh, atau izin yang diberikan diam-diam |
| Izin ditolak | UI tetap dapat digunakan dan tidak mengaku siap memantau |
| Pemilihan sumber | Hanya aplikasi yang dipilih dan diaktifkan diproses; aplikasi lain tidak disimpan/dikirim |
| Konfigurasi | HTTP, query, fragment, dan userinfo ditolak; domain ditinjau sebelum simpan |
| Pembayaran nyata | Invoice dibuat melalui website, nominal dibayar tepat; mutasi, riwayat, deposit, dan saldo cocok; hanya satu kredit/ledger |
| Duplikat/retry | Update notifikasi dan retry tidak menggandakan saldo; gunakan lingkungan terisolasi untuk replay |
| Offline/pulih | Antrean bertahan dan terkirim saat jaringan pulih; hapus riwayat tidak menghapus pending |
| Jeda | Pemantauan/sumber nonaktif menghentikan pengiriman berikutnya; permintaan in-flight mungkin selesai |
| Perubahan tujuan | Endpoint/token baru menahan antrean lama, tidak mengalihkannya otomatis |
| Hasil server | 401/404/422 memerlukan perhatian; 5xx/429/jaringan dicoba ulang secara terbatas |
| Hasil deposit | ignored/unmatched/ambiguous/review/duplicate tidak dianggap kredit saldo |
| Filter | Promo, OTP, transaksi keluar/gagal tidak dianggap pembayaran masuk |
| Background | Uji layar mati, Doze, reboot, dan baterai; catat pengaturan OEM serta keterlambatan |
| Listener | Cabut izin dan aktifkan lagi; amati status serta pemulihan sambungan |
| Tampilan | Semua tab, daftar panjang/scroll, font besar, keyboard, rotasi, dan masking token |
| Pembaruan | APK dengan sertifikat sama dan versionCode lebih tinggi mempertahankan konfigurasi/pending |
| Privasi | Tidak ada token/payload di log, source, backup, atau screenshot yang dibagikan |

Force stop dapat membutuhkan pembukaan ulang aplikasi. Tidak ada pengaturan OEM yang menjamin pengiriman real-time tanpa gangguan.

## Batas kontrak backend

Fingerprint backend saat diperiksa tidak menggunakan ID transaksi/waktu pembayaran tepercaya. Pembayaran berulang dengan akun/package/judul/nominal identik dapat dianggap duplikat; nominal yang memiliki invoice historis dapat ditahan untuk review. Jangan melonggarkan pengaman untuk membuat tes lulus; rekonsiliasi dan konfirmasi admin dilakukan setelah memeriksa mutasi.

## Pemeriksaan otomatis pengembang

Jalankan dari root repository dengan JDK 17 dan Android SDK 35:

```sh
./gradlew testDebugUnitTest lintRelease assembleDebug assembleDebugAndroidTest
python3 scripts/check-release.py
python3 scripts/check-startup.py
```

Untuk artefak bertanda tangan, `python3 scripts/build-release.py assembleDebug assembleDebugAndroidTest` menjalankan unit test, lint, build release, dan verifikasi signature. Ikuti catatan signing pada [README](README.md); jangan mengganti identitas resmi.

### Emulator terisolasi

Helper emulator memakai toolchain lokal di `$HOME/.local/share/panelin-android-toolchain` dengan JDK 17, command-line tools, SDK 35/Build Tools 35.0.0, emulator, dan image Android 26 x86_64. `setup-local-sdk.py` memasang paket SDK setelah struktur toolchain tersedia; bukan installer lengkap JDK/Gradle. Sesuaikan lokasi helper bila workstation Anda berbeda.

```sh
python3 scripts/start-emulator.py
# Di terminal lain, setelah APK debug/instrumentation dibangun:
python3 scripts/device-check.py
# Untuk pemeriksaan navigasi APK release yang telah dipasang:
python3 scripts/check-release-ui.py
```

**Jangan jalankan helper ini pada HP client berdata nyata.** Serial emulator dipatok ke `emulator-5580`; device-check membersihkan data/uninstall aplikasi fixture. Setelah seluruh tes lulus, helper memasang APK release. Jika suite terhenti lebih awal, langkah akhir itu tidak dijalankan.

Hasil per kelas dan screenshot lokal ada di `dist/qa/` (tidak di-commit). Host tanpa KVM dapat mengalami timeout/ANR sistem; periksa screenshot/hierarki dan jangan mengklaim tes yang terhalang telah lulus.

- `StartupTest`: membuka Activity dan memeriksa judul dashboard, tanpa bergantung pada UI Automator atau izin notifikasi. Jalankan juga pada Android 15/API 35 untuk jalur system bar modern.
- `check-startup.py`: guard urutan inisialisasi source, bukan pengganti tes runtime.
- `StoreSecurityTest`: SQLite/Keystore nyata dan penolakan URL tidak aman.
- `QueueBehaviorTest`: pending, pause, hapus riwayat, dan credential-change hold. Bukan simulasi ketahanan offline menyeluruh.
- `StatusNotificationTest`: status tidak mengaku tersambung tanpa akses listener.
- `WorkerContractTest`: worker/JSON/outcome nyata melalui transport in-memory, tanpa request eksternal.
- `ClientSmokeTest`: navigasi tanpa izin sensitif dan tanpa data bawaan; keterbatasan hasil terakhir dicatat di QA.

Pemeriksaan release langsung memverifikasi judul halaman melalui hierarki aksesibilitas. Pemeriksaan ini tidak membuktikan pemrosesan pembayaran end-to-end.
