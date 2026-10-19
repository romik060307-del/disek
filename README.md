# WA Cloning — Android WebView Wrapper

Aplikasi Android sederhana yang membuka situs **resmi** `https://web.whatsapp.com/` dalam WebView, dengan mode desktop, penyimpanan cookie WebView, pemilih file untuk upload, dan unduhan standar untuk URL HTTP/HTTPS yang diberikan halaman.

> **Penting:** Ini adalah proyek pihak ketiga, bukan aplikasi resmi atau afiliasi WhatsApp. WhatsApp memperingatkan bahwa menautkan akun melalui aplikasi/situs yang tidak resmi dapat berisiko terhadap akun. Baca: https://faq.whatsapp.com/378279804439436/

## Fitur

- Langsung membuka WhatsApp Web; tidak membutuhkan situs backend atau Netlify.
- Mode desktop sebagai pengaturan awal, dengan pilihan mode seluler.
- Cookie dikelola oleh Android WebView dan tidak sengaja dihapus saat aplikasi ditutup.
- Tombol muat ulang dan opsi membuka WhatsApp Web di browser.
- Pemilih file Android untuk mengunggah lampiran.
- Unduhan melalui Android DownloadManager saat halaman menyediakan URL HTTP/HTTPS langsung.
- Permintaan izin kamera/mikrofon hanya saat halaman meminta fitur tersebut dan pengguna menyetujuinya.
- Ikon aplikasi memakai file RK yang disertakan oleh pemilik proyek.

## Batasan yang harus diketahui

- **Tidak ada jaminan anti-logout.** WhatsApp dapat meminta tautan ulang, mencabut sesi, atau menerapkan batas perangkat dan kebijakan keamanan. Cookie lokal membantu mempertahankan sesi, tetapi tidak bisa memaksa WhatsApp tetap menautkan akun.
- WhatsApp dapat memblokir atau mengubah dukungan untuk browser/WebView pihak ketiga. Jika QR tidak muncul atau halaman tidak berjalan baik, pilih **Buka browser** lalu coba browser yang didukung, atau gunakan aplikasi resmi.
- Unduhan hanya berlaku untuk tautan unduhan biasa yang diekspos halaman. Sebagian media disajikan melalui URL `blob:` atau alur internal sehingga Android DownloadManager tidak dapat mengambilnya. Fitur ini **tidak** membobol atau menyimpan pesan sekali lihat, media privat tersembunyi, atau konten yang sengaja dibatasi.
- Aplikasi membutuhkan internet. Ia tidak menyimpan salinan chat sendiri; penyimpanan sesi dikelola WebView dan WhatsApp.
- Karena ini APK debug untuk uji coba, jangan anggap file tersebut sebagai rilis Play Store yang siap produksi.

## Bangun APK otomatis di GitHub

1. Buat repository baru di GitHub.
2. Upload semua isi folder proyek ini ke repository (file `.github/workflows/build-apk.yml` harus ikut).
3. Buka tab **Actions**, pilih **Build WA Cloning APK**, tekan **Run workflow**. Build juga berjalan saat ada push ke `main` atau `master`.
4. Setelah workflow hijau, buka hasil run tersebut dan unduh **Artifacts → wa-cloning-apk**. Ekstrak ZIP artifact untuk mendapatkan `app-debug.apk`.
5. Pindahkan APK ke HP Android dan instal. Jika Android bertanya, izinkan pemasangan dari sumber tersebut hanya jika kamu mempercayai APK buatanmu sendiri.

**Jangan upload ZIP ini sebagai satu file lalu berharap langsung menjadi APK.** Ekstrak ZIP di komputer, lalu upload isi folder proyek (termasuk folder `.github`) ke repository GitHub.

Untuk membuat halaman GitHub Release, buat tag versi misalnya `v1.0.0` dan push tag itu. Workflow akan melampirkan APK debug ke release.

## Build lokal (opsional)

Jika membangun di komputer sendiri, siapkan JDK 17, Gradle 8.13, dan Android SDK 36, lalu jalankan dari folder proyek: `gradle --no-daemon :app:assembleDebug`. APK hasil build berada di `app/build/outputs/apk/debug/app-debug.apk`. Cara paling mudah untuk pemula tetap memakai GitHub Actions di atas. Versi konfigurasi menggunakan Android Gradle Plugin 8.13.2.

## Menautkan WhatsApp

1. Buka WA Cloning dan tunggu QR WhatsApp Web muncul.
2. Di aplikasi WhatsApp utama, buka **Perangkat tertaut → Tautkan perangkat**.
3. Pindai QR menggunakan ponsel utama. Jangan bagikan QR atau kode penautan ke orang lain.

WhatsApp Web juga menyediakan opsi penautan dengan nomor telepon pada browser/perangkat yang didukung; opsi ini ditentukan oleh situs WhatsApp dan tidak dibuat oleh aplikasi ini.

## Privasi dasar

Proyek ini tidak mempunyai server sendiri, tidak meminta kata sandi WhatsApp, dan tidak mengirim data ke backend buatan pengembang. Namun, semua interaksi WhatsApp berlangsung pada situs WhatsApp dan tunduk pada kebijakan serta keamanan WhatsApp. Unduh hanya media yang memang berhak kamu simpan.
