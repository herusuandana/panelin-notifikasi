# Panelin Notifikasi

Aplikasi Android untuk pemilik/admin website **Panelin**, bukan pelanggan yang melakukan deposit. Aplikasi meneruskan notifikasi pembayaran dari HP ke webhook website untuk membantu konfirmasi deposit.

Setiap client memilih sumber, URL, dan token sendiri. Tidak ada endpoint bawaan, server perantara Panelin, iklan, atau telemetri terpusat.

v1.4.0 tersedia sebagai prerelease untuk uji terbatas. Screenshot di bawah masih menampilkan v1.2.0, bukan preview versi terbaru.

## Unduh dan pasang

**[Unduh APK v1.4.0 — uji terbatas](https://github.com/herusuandana/panelin-notifikasi/releases/tag/v1.4.0)**

Di **Assets**, pilih `panelin-notifikasi-1.4.0-release.apk`, bukan **Source code**. File `.sha256` tersedia untuk memeriksa integritas.

- Minimum Android 8.0, package `id.panelin.notifikasi`, sertifikat release tetap, bukan debug key.
- UI native bergaya macOS/iOS dan timeline aktivitas. Jejak terenkripsi mencatat skip lokal, percobaan webhook, HTTP, hasil terpilih, dan retry tanpa respons mentah atau token.
- Belum siap produksi. Matriks responsif, navigasi APK signed v1.4.0, Android 15, HP fisik, dan pembayaran hingga kredit saldo belum terverifikasi lengkap. Lihat [hasil QA](QA-RESULTS.md).
- Versi lain tersedia di [GitHub Releases](https://github.com/herusuandana/panelin-notifikasi/releases). Aplikasi belum memiliki pembaruan otomatis.

<p>
  <img src="docs/images/beranda.png" width="240" alt="Beranda Panelin Notifikasi dengan penjelasan privasi dan navigasi bawah">
  <img src="docs/images/riwayat.png" width="240" alt="Riwayat pengiriman dengan filter dan keadaan kosong tanpa transaksi contoh">
</p>

v1.0.1 memperbaiki urutan inisialisasi window yang dapat menyebabkan crash v1.0.0 pada Android 15. Crash HP pelapor belum dikonfirmasi selesai lewat log atau uji perangkat. Pasang pembaruan **tanpa uninstall** agar konfigurasi dan antrean tetap tersimpan. Package dan sertifikat harus sama.

## Fitur

- Cari aplikasi terpasang dan atur sumber, URL webhook, serta token per aplikasi.
- Pantau melalui akses notifikasi Android, tanpa login ke dompet.
- Antrean persisten terenkripsi dengan Android Keystore dan retry terbatas. Perubahan tujuan/token menahan antrean lama, bukan mengalihkannya.
- Riwayat membedakan `credited`, `duplicate`, `ignored`, `unmatched`, `ambiguous`, dan `review`.
- Panduan izin, baterai, autostart, versi, dan pembaruan.

## Penyiapan client

1. Unduh APK resmi dan izinkan pemasangan dari sumber tersebut jika diminta Android.
2. Baca penjelasan privasi. Untuk aplikasi sideload, Android tertentu meminta **Allow restricted settings / Izinkan setelan terbatas** di info aplikasi sebelum akses notifikasi bisa diaktifkan. Nama menu berbeda tiap perangkat. Aplikasi tidak melewati pembatasan Android.
3. Di website Panelin, aktifkan konfirmasi notifikasi pada metode pembayaran Manual yang mendukungnya. Salin URL callback dan token penerima.
4. Di tab **Aplikasi**, pilih sumber pembayaran, lalu **Hubungkan aplikasi** atau **Kelola tujuan**. Isi URL HTTPS dan token, lalu berikan persetujuan. Format URL: `https://DOMAIN-ANDA/callback/notification/ID_METODE`, dengan ID numerik.
5. Dari **Beranda**, buka pengaturan Android untuk memberi akses notifikasi. Izinkan notifikasi status, aktifkan penerusan, lalu tinjau baterai/autostart sesuai merek HP.
6. Ikuti [checklist uji perangkat](TESTING.md) sebelum dipakai. Untuk pembayaran nyata yang disetujui client, cocokkan Riwayat dengan invoice, mutasi dompet, dan saldo website.

**Jangan masukkan PIN, password, atau OTP DANA.** Gunakan token webhook dari metode pembayaran website, bukan kredensial dompet atau akun admin.

## Kompatibilitas dan batas

Preset backend yang diperiksa adalah **DANA Bisnis**: package `id.dana`, judul `Pembayaran Masuk`, teks `Rp{nominal} diterima DANA Bisnis.`. Parser harus cocok dengan notifikasi asli. Memilih aplikasi lain tidak otomatis mendukung formatnya. DANA pribadi dan format lain perlu diuji terpisah.

- **HTTP 200 bukan bukti kredit saldo.** Hanya `credited` berarti server mengonfirmasi kredit.
- Status listener hanya menunjukkan keadaan HP, bukan kesehatan server atau keaslian dana masuk.
- Notifikasi bukan API resmi bank/dompet. Jaga perangkat dan token. Perangkat terkompromi dapat memalsukan klaim.
- Backend dapat menahan nominal historis untuk review atau menganggap akun/package/judul/nominal berulang sebagai duplikat. Android tidak mengubah pengaman ini.
- Doze, penghemat baterai, pembatasan OEM, dan force stop dapat menunda atau menghentikan pemrosesan. Tidak ada jaminan aktif tanpa gangguan selama 24 jam.
- Tidak ada tombol pembayaran palsu untuk tes koneksi. Payload yang cocok bisa menambah saldo.

## Privasi dan bantuan

Baca [privasi](PRIVACY.md) dan [catatan pembaruan](CHANGELOG.md). Jika HP hilang, rotasi token di website dan amankan perangkat.

Hubungi pengelola Panelin atau [buat issue](https://github.com/herusuandana/panelin-notifikasi/issues) dengan versi aplikasi, Android, merek/model, dan langkah reproduksi. **Issue GitHub bersifat publik. Jangan unggah token, isi notifikasi pembayaran, data pelanggan, database, atau kunci signing.**

## Untuk pengembang

UI Jetpack Compose dan layanan native Kotlin, tanpa JavaScript atau WebView. Gunakan JDK 17, SDK 35, Build Tools 35.0.0, dan Gradle Wrapper 8.11.1. Versi dependensi ditetapkan di file Gradle.

```sh
git clone https://github.com/herusuandana/panelin-notifikasi.git
cd panelin-notifikasi
# Atur JAVA_HOME ke JDK 17 dan ANDROID_HOME ke Android SDK lokal.
./gradlew testDebugUnitTest lintRelease assembleDebug
python3 scripts/check-release.py
```

`python3 scripts/build-release.py` menjalankan unit test, lint, build release, dan verifikasi signature. Skrip membuat identitas signing lokal sekali di `$HOME/.local/share/panelin-android-signing`, di luar repository. **Kunci lokal baru tidak dapat memperbarui APK resmi.** Pembaruan resmi harus memakai kunci resmi. Cadangkan kunci secara offline, jangan unggah ke GitHub.

Hasil build ada di `dist/`. `python3 scripts/package-source.py` membuat arsip source tanpa cache, data lokal, atau keystore. Panduan SDK dan emulator ada di [TESTING.md](TESTING.md).

### Kontrak webhook

`POST /callback/notification/{ID_METODE}` memakai JSON `sign`, `pkg`, `title`, `text`, `bigtext`. Token hanya dikirim dalam body HTTPS. Query, fragment, userinfo, dan redirect ditolak. Backend mencocokkan invoice dan menentukan kredit. Aplikasi hanya meneruskan kejadian.

### Merilis pembaruan

Naikkan `versionCode` dan `versionName` di `app/build.gradle.kts`. Selaraskan `VERSION`, `CHANGELOG.md`, dan `app/src/main/assets/changelog.json`, lalu jalankan pemeriksaan dan QA. Gunakan sertifikat release yang sama dan unggah APK beserta checksum ke GitHub Releases. Pasang di atas versi lama. **Jangan uninstall jika masih ada antrean.**
