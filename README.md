# Panelin Notifikasi

Aplikasi Android untuk pemilik/admin website Panelin, bukan pelanggan yang deposit. Aplikasi meneruskan notifikasi pembayaran dari HP ke webhook website. Setiap client memilih sumber, URL, dan token sendiri, tanpa server perantara, iklan, atau telemetri Panelin.

v1.6.0 (versionCode 8, 2026-10-28) adalah prerelease uji terbatas. QA runtime belum lengkap. Screenshot di bawah berasal dari APK signed v1.6.0.

## Unduh dan pasang

**[Unduh APK v1.6.0 — uji terbatas](https://github.com/herusuandana/panelin-notifikasi/releases/tag/v1.6.0)**

Di **Assets**, pilih `panelin-notifikasi-1.6.0-release.apk`, bukan **Source code**. Periksa file `.sha256`. Minimum Android 8.0, package `id.panelin.notifikasi`, sertifikat release tetap.

Belum siap produksi. Navigasi empat tab APK signed v1.6.0 lulus di API 26. Tes updater lulus setelah rerun, tetapi tes UI/editor belum seluruhnya lulus. Pemasangan update, Android 15, HP fisik, dan pembayaran hingga kredit saldo belum terverifikasi. Lihat [hasil QA](QA-RESULTS.md) dan [rilis lain](https://github.com/herusuandana/panelin-notifikasi/releases).

Screenshot v1.6.0:

<p>
  <img src="docs/images/beranda.png" width="240" alt="Beranda Panelin Notifikasi v1.6.0">
  <img src="docs/images/riwayat.png" width="240" alt="Riwayat Panelin Notifikasi v1.6.0 tanpa transaksi contoh">
</p>

Pasang pembaruan tanpa uninstall agar konfigurasi dan antrean tersimpan. Package dan sertifikat harus sama. v1.0.1 memperbaiki urutan startup, tetapi crash HP pelapor di Android 15 belum dikonfirmasi selesai.

## Fitur dan pembaruan v1.6.0

- Pilih aplikasi sumber, URL webhook, dan token per aplikasi, tanpa login ke dompet.
- Antrean persisten terenkripsi dengan Android Keystore dan retry terbatas. Perubahan URL/token menahan antrean lama, bukan mengalihkannya.
- Riwayat membedakan `credited`, `duplicate`, `ignored`, `unmatched`, `ambiguous`, dan `review`. Jejak terenkripsi mencatat skip, HTTP, dan retry tanpa token atau respons mentah.
- Empat halaman native bergaya macOS yang lebih ringkas, tarik untuk memuat ulang, skeleton saat memuat, pilihan khusus, dan ikon package sumber di Riwayat.
- Cek rilis GitHub setiap aplikasi dibuka atau kembali ke depan. Cek otomatis diam jika tidak ada versi baru atau jaringan gagal. Popup hanya untuk rilis lebih baru yang diizinkan. Bawaan hanya rilis stabil, rilis uji bisa diaktifkan di Pengaturan.
- Unduhan dimulai atas pilihan pengguna, lalu Android meminta konfirmasi pemasangan. APK disimpan privat dan dibagikan lewat FileProvider khusus pembaruan. SHA-256, package, versi, dan signature diperiksa sebelum pemasangan.

## Penyiapan

1. Pasang APK resmi. Jika akses notifikasi terkunci, buka info aplikasi → **Allow restricted settings / Izinkan setelan terbatas**. Menu berbeda tiap HP, aplikasi tidak melewati pembatasan Android.
2. Di metode pembayaran Manual website Panelin yang mendukung konfirmasi notifikasi, salin URL callback dan token penerima.
3. Di **Aplikasi**, pilih sumber → **Hubungkan aplikasi** atau **Kelola tujuan**. Isi URL HTTPS dan token, periksa penerimanya, lalu setujui pengiriman. Format: `https://DOMAIN-ANDA/callback/notification/ID_METODE`, ID numerik.
4. Di **Pengaturan**, izinkan akses notifikasi dan tinjau baterai/autostart. Aktifkan penerusan di **Beranda**.
5. Ikuti [panduan uji](TESTING.md). Untuk pembayaran nyata yang disetujui client, cocokkan Riwayat, invoice, mutasi dompet, dan saldo website.

Jangan masukkan PIN, password, atau OTP dompet. Gunakan token webhook website, bukan kredensial dompet/admin.

## Batas penggunaan

Preset backend yang diperiksa adalah DANA Bisnis: package `id.dana`, judul `Pembayaran Masuk`, teks `Rp{nominal} diterima DANA Bisnis.`. Parser harus cocok dengan notifikasi asli. Memilih aplikasi lain tidak menambah dukungan format. DANA pribadi perlu diuji terpisah.

- HTTP 200 bukan bukti kredit saldo. Hanya `credited` mengonfirmasi kredit dari server. Status listener hanya menunjukkan sambungan HP, bukan kesehatan server atau keaslian dana.
- Notifikasi bukan API resmi bank/dompet. HP terkompromi bisa memalsukan klaim. Lindungi perangkat dan token.
- Backend bisa menahan nominal historis untuk review atau menganggap akun/package/judul/nominal berulang sebagai duplikat. Android tidak mengubah pengaman ini.
- Doze, baterai, pembatasan OEM, dan force stop bisa menunda atau menghentikan pemrosesan. Tidak ada jaminan aktif 24 jam.
- Tidak ada tes pembayaran palsu. Payload yang cocok bisa menambah saldo.

## Privasi dan bantuan

Baca [privasi](PRIVACY.md) dan [catatan pembaruan](CHANGELOG.md). GitHub menerima IP dan metadata koneksi saat cek rilis, bukan token atau isi notifikasi. Jika HP hilang, rotasi token di website.

Hubungi pengelola Panelin atau [buat issue](https://github.com/herusuandana/panelin-notifikasi/issues) dengan versi, Android, merek/model, dan langkah reproduksi. Issue bersifat publik. Jangan unggah token, notifikasi pembayaran, data pelanggan, database, atau kunci signing.

## Pengembang

Jetpack Compose dan Kotlin native, tanpa JavaScript/WebView. Gunakan JDK 17, SDK 35, Build Tools 35.0.0, dan Gradle Wrapper 8.11.1. Dependensi ditetapkan di Gradle.

```sh
git clone https://github.com/herusuandana/panelin-notifikasi.git
cd panelin-notifikasi
# Atur JAVA_HOME ke JDK 17 dan ANDROID_HOME ke SDK lokal.
./gradlew testDebugUnitTest lintRelease assembleDebug
python3 scripts/check-release.py
```

`python3 scripts/build-release.py` menjalankan unit test, lint, build release, dan verifikasi signature. Kunci lokal dibuat sekali di `$HOME/.local/share/panelin-android-signing`, di luar repo. Kunci baru tidak bisa memperbarui APK resmi. Gunakan kunci resmi yang sama, cadangkan offline, jangan unggah ke GitHub.

Build ada di `dist/`. `python3 scripts/package-source.py` mengarsipkan source tanpa cache, data lokal, atau keystore. Lihat [panduan SDK dan emulator](TESTING.md).

### Webhook

`POST /callback/notification/{ID_METODE}` memakai JSON `sign`, `pkg`, `title`, `text`, `bigtext`. Token hanya di body HTTPS. Query, fragment, userinfo, dan redirect ditolak. Backend mencocokkan invoice dan menentukan kredit, aplikasi hanya meneruskan kejadian.

### Rilis

Naikkan `versionCode`/`versionName` di `app/build.gradle.kts`. Selaraskan `VERSION`, `CHANGELOG.md`, dan `app/src/main/assets/changelog.json`. Jalankan pemeriksaan dan QA, lalu unggah APK bersertifikat resmi beserta checksum ke GitHub Releases. Jangan uninstall jika masih ada antrean.
